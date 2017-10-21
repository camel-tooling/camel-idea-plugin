/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import java.io.File;
import java.io.IOException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ModuleTestCase;
import com.intellij.util.ui.UIUtil;
import org.apache.camel.idea.service.CamelService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Test if the {@link CamelService} service is updated correctly when changes happen to
 * the Project and model configuration
 */
public class CamelProjectComponentTestIT extends ModuleTestCase {

    private File root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = new File(FileUtil.getTempDirectory());
    }

    public void testAddLibrary() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        File camelJar = createTestArchive("camel-core-2.19.0.jar");
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(camelJar);

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);
        addLibraryToModule(virtualFile, projectLibraryTable, "Maven: org.apache.camel:camel-core:2.19.0-snapshot");

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
        assertEquals(true, service.isCamelPresent());
    }

    public void testRemoveLibrary() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        VirtualFile camelCoreVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive("camel-core-2.19.0.jar"));
        VirtualFile camelSpringVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive("camel-spring-2.19.0.jar"));

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);

        Library springLibrary = addLibraryToModule(camelSpringVirtualFile, projectLibraryTable, "Maven: org.apache.camel:camel-spring:2.19.0-snapshot");
        Library coreLibrary = addLibraryToModule(camelCoreVirtualFile, projectLibraryTable, "Maven: org.apache.camel:camel-core:2.19.0-snapshot");

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(2, service.getLibraries().size());
        assertEquals(true, service.isCamelPresent());

        ApplicationManager.getApplication().runWriteAction(() -> projectLibraryTable.removeLibrary(springLibrary));

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
    }

    public void testAddModule() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        File camelJar = createTestArchive("camel-core-2.19.0.jar");
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(camelJar);

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);
        ApplicationManager.getApplication().runWriteAction(() -> {
            final Module moduleA = createModule("myNewModel.iml");
            Library library = projectLibraryTable.createLibrary("Maven: org.apache.camel:camel-core:2.19.0-snapshot");
            final Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
            libraryModifiableModel.addRoot(virtualFile, OrderRootType.CLASSES);
            libraryModifiableModel.commit();
            ModuleRootModificationUtil.addDependency(moduleA, library);
        });

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
    }

    public void testAddLegacyPackaging() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        VirtualFile camelCoreVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive("camel-core-2.19.0.jar"));
        VirtualFile legacyJarPackagingFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive("legacy-custom-file-0.12.snapshot.jar"));

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);

        addLibraryToModule(camelCoreVirtualFile, projectLibraryTable, "Maven: org.apache.camel:camel-core:2.19.0-snapshot");
        addLibraryToModule(legacyJarPackagingFile, projectLibraryTable, "c:\\test\\libs\\legacy-custom-file-0.12.snapshot.jar");

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
        assertEquals(true, service.getLibraries().contains("camel-core"));
    }

    public void testAddLibWithoutMavenPackaging() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        VirtualFile camelCoreVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive("camel-core-2.19.0.jar"));

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);

        addLibraryToModule(camelCoreVirtualFile, projectLibraryTable, "org.apache.camel:camel-core:2.19.0-snapshot");

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
        assertEquals(true, service.getLibraries().contains("camel-core"));

    }

    private File createTestArchive(String filename) throws IOException {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, filename)
            .addClasses(CamelService.class);
        File file = new File(root, archive.getName());
        file.deleteOnExit();
        archive.as(ZipExporter.class).exportTo(file, true);
        return file;
    }

    private Library addLibraryToModule(VirtualFile camelSpringVirtualFile, LibraryTable projectLibraryTable, String name) {
        return ApplicationManager.getApplication().runWriteAction((Computable<Library>)() -> {
            Library library = projectLibraryTable.createLibrary(name);
            Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
            libraryModifiableModel.addRoot(camelSpringVirtualFile, OrderRootType.CLASSES);
            libraryModifiableModel.commit();
            ModuleRootModificationUtil.addDependency(myModule, library);
            return library;
        });
    }
}