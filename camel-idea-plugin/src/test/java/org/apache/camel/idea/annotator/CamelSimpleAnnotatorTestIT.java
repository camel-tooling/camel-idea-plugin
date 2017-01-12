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
package org.apache.camel.idea.annotator;

import java.io.File;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;


/**
 * Test Camel simple validation and the expected value is highlighted
 * TIP : Writing highlighting test can be tricky because if the highlight is one character off
 * it will fail, but the error messaged might still be correct. In this case it's likely the TextRange
 * is incorrect.
 */
public class CamelSimpleAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public static final String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:2.19.0-SNAPSHOT";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File[] mavenArtifacts = getMavenArtifacts(CAMEL_CORE_MAVEN_ARTIFACT);
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(mavenArtifacts[0]);
        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myModule.getProject());
        ApplicationManager.getApplication().runWriteAction(() -> {
            Library library = projectLibraryTable.createLibrary("Maven: " + CAMEL_CORE_MAVEN_ARTIFACT);
            final Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
            libraryModifiableModel.addRoot(virtualFile, OrderRootType.CLASSES);
            libraryModifiableModel.commit();
            ModuleRootModificationUtil.addDependency(myModule, library);
        });
        UIUtil.dispatchAllInvocationEvents();
    }

    public void testAnnotatorSimpleValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithSimple());
        myFixture.checkHighlighting(false, false, true, true);
    }

    private String getJavaWithSimple() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"netty-http:http://localhost/cdi?matchOnUriPrefix=true&nettySharedHttpServer=#httpServer\")\n"
            + "            .id(\"http-route-cdi\")\n"
            + "            .transform()\n"
            + "            .simple(\"Response from Camel CDI on route<error descr=\"Unknown function: xrouteId\"> ${xrouteId} using thread: ${threadName}</error>\");"
            + "        }\n"
            + "    }";
    }
}