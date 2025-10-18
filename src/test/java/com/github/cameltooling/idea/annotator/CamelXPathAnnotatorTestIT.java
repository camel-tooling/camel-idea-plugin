/*
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
package com.github.cameltooling.idea.annotator;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;

/**
 * Test Camel XPath validation and the expected value is highlighted
 *
 * @see CamelSimpleAnnotatorTestIT
 */
public class CamelXPathAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public static final String CAMEL_XPATH_MAVEN_ARTIFACT = "org.apache.camel:camel-xpath:4.14.1";

    public CamelXPathAnnotatorTestIT() {
        setIgnoreCamelCoreLib(true);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File[] mavenArtifacts = getMavenArtifacts(CAMEL_XPATH_MAVEN_ARTIFACT);
        for (File file : mavenArtifacts) {
            final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(getModule().getProject());
            ApplicationManager.getApplication().runWriteAction(() -> {
                String name = file.getName();
                Library library = projectLibraryTable.createLibrary("maven: " + name);
                final Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
                libraryModifiableModel.addRoot(virtualFile, OrderRootType.CLASSES);
                libraryModifiableModel.commit();
                ModuleRootModificationUtil.addDependency(getModule(), library);
            });
        }

        disposeOnTearDown(getModule().getProject().getService(CamelCatalogService.class));
        disposeOnTearDown(getModule().getProject().getService(CamelService.class));
        getModule().getProject().getService(CamelService.class).setCamelPresent(true);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } catch (Throwable e) {
            // ignore
        }
    }

    protected static File[] getMavenArtifacts(String... mavenAritfiact) throws IOException {
        File[] libs = Maven.resolver().resolve(mavenAritfiact).withTransitivity().asFile();
        return libs;
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator";
    }

    public void testAnnotatorXPathValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithXPath());
        myFixture.checkHighlighting(false, false, true, true);
    }

    private String getJavaWithXPath() {
        return """
                import org.apache.camel.builder.RouteBuilder;
                public class MyRouteBuilder extends RouteBuilder {
                        public void configure() throws Exception {
                            from("direct:start")
                                .filter(xpath("\\/path/to"))
                                    .to("mock:match")
                                .end();
                        }
                    }""";
    }

}
