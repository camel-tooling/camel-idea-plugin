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
import java.util.Arrays;
import java.util.List;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.service.CamelService;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * Super class for Camel Plugin Testing. If you are testing plug-in code with LightCodeInsightFixtureTestCase
 * you should extend this class to make sure it is setup as expected and clean up on tearDown
 */
public abstract class CamelLightCodeInsightFixtureTestCaseIT extends LightCodeInsightFixtureTestCase {

    private static final String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:2.19.0";

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
        if (!ignoreCamelCoreLib) {
            PsiTestUtil.addLibrary(myModule, "Maven: " + CAMEL_CORE_MAVEN_ARTIFACT, mavenArtifacts[0].getParent(), mavenArtifacts[0].getName());
        }
        disposeOnTearDown(ServiceManager.getService(myModule.getProject(), CamelCatalogService.class));
        disposeOnTearDown(ServiceManager.getService(myModule.getProject(), CamelService.class));
        ServiceManager.getService(myModule.getProject(), CamelService.class).setCamelPresent(true);

    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }

    /**
     * Get a list of artifact declared as dependencies in the pom.xml file.
     * <p>
     *   The method take a String arrays off "G:A:P:C:?" "org.apache.camel:camel-core:2.19.0"
     * </p>
     * @param mavneAritfiact - Array of maven artifact to resolve
     * @return Array of artifact files
     * @throws IOException
     */
    private static File[] getMavenArtifacts(String... mavneAritfiact) throws IOException {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
            .resolve(mavneAritfiact)
            .withoutTransitivity().asFile();

        return libs;
    }

    protected PsiElement getElementAtCaret() {
        int offset = myFixture.getCaretOffset();
        if (offset < 0) {
            fail("No <caret> found");
        }
        return myFixture.getFile().findElementAt(offset);
    }

    protected void setIgnoreCamelCoreLib(boolean ignoreCamelCoreLib) {
        this.ignoreCamelCoreLib = ignoreCamelCoreLib;
    }

    protected void initCamelPreferencesService() {
        List<String> expectedExcludedProperties = Arrays.asList("**/log4j.properties", "**/log4j2.properties", "**/logging.properties");
        CamelPreferenceService service = ServiceManager.getService(CamelPreferenceService.class);
        service.setExcludePropertyFiles(expectedExcludedProperties);
        service.setRealTimeEndpointValidation(true);
        service.setScanThirdPartyLegacyComponents(true);
        service.setCustomIconFilePath("");
        service.setDownloadCatalog(true);
        service.setHighlightCustomOptions(true);

        List<String> expectedIgnoredProperties = Arrays.asList("java.", "Logger.", "logger", "appender.", "rootLogger.",
                "camel.springboot.", "camel.component.", "camel.dataformat.", "camel.language.");
        service.setIgnorePropertyList(expectedIgnoredProperties);
        service.setShowCamelIconInGutter(true);
        service.setScanThirdPartyComponents(true);
        service.setRealTimeSimpleValidation(true);
        service.setChosenCamelIcon("Camel Icon");
    }
}
