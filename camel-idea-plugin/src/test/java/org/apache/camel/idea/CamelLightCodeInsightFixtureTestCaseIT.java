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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelService;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jetbrains.annotations.NotNull;


/**
 * Super class for Camel Plugin Testing. If you are testing plug-in code with LightCodeInsightFixtureTestCase
 * you should extend this class to make sure it is setup as expected and clean up on tearDown
 */
public abstract class CamelLightCodeInsightFixtureTestCaseIT extends LightCodeInsightFixtureTestCase {

    public static final String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:2.19.0-SNAPSHOT";
    private static File[] mavenArtifacts;
    private boolean ignoreCamelCoreLib;

    static {
        try {
            mavenArtifacts = getMavenArtifacts(CAMEL_CORE_MAVEN_ARTIFACT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        disposeOnTearDown(ServiceManager.getService(myModule.getProject(), CamelCatalogService.class));
        disposeOnTearDown(ServiceManager.getService(myModule.getProject(), CamelService.class));
        ServiceManager.getService(myModule.getProject(), CamelService.class).setCamelPresent(true);
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {

        return new DefaultLightProjectDescriptor() {
            @Override
            public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
                super.configureModule(module, model, contentEntry);
                if (!isIgnoreCamelCoreLib()) {
                    PsiTestUtil.addLibrary(module, model, "Maven: " + CAMEL_CORE_MAVEN_ARTIFACT, mavenArtifacts[0].getParent(), mavenArtifacts[0].getName());
                }
            }
        };
    }
    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }

    /**
     * Get a list of artifact declared as dependencies in the pom.xml file.
     * <p>
     *   The method take a String arrays off "G:A:P:C:?" "org.apache.camel:camel-core:2.19.0-SNAPSHOT"
     * </p>
     * @param mavneAritfiact - Array of maven artifact to resolve
     * @return Array of artifact files
     * @throws IOException
     */
    protected static File[] getMavenArtifacts(String... mavneAritfiact) throws IOException {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
            .resolve(mavneAritfiact)
            .withoutTransitivity().asFile();

        return libs;
    }

    public boolean isIgnoreCamelCoreLib() {
        return this.ignoreCamelCoreLib;
    }

    public void setIgnoreCamelCoreLib(boolean ignoreCamelCoreLib) {
        this.ignoreCamelCoreLib = ignoreCamelCoreLib;
    }
}
