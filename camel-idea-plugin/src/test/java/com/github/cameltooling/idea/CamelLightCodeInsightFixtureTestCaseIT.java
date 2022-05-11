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
package com.github.cameltooling.idea;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.JavaMethodUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JpsJavaSdkType;

/**
 * Super class for Camel Plugin Testing. If you are testing plug-in code with LightCodeInsightFixtureTestCase
 * you should extend this class to make sure it is setup as expected and clean up on tearDown
 */
public abstract class CamelLightCodeInsightFixtureTestCaseIT extends LightJavaCodeInsightFixtureTestCase {
    private static final String BUILD_MOCK_JDK_DIRECTORY = "build/mockJDK-";

    private static File[] mavenArtifacts;
    private boolean ignoreCamelCoreLib;

    protected static String CAMEL_VERSION;
    protected static String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:%s";


    static {
        try {
            final String projectRoot = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").lastIndexOf('/'));

            Properties gradleProperties = new Properties();
            gradleProperties.load(new FileInputStream(projectRoot +"/gradle.properties"));
            CAMEL_VERSION = gradleProperties.getProperty("camelVersion");
            CAMEL_CORE_MAVEN_ARTIFACT = String.format(CAMEL_CORE_MAVEN_ARTIFACT, CAMEL_VERSION);

            mavenArtifacts = getMavenArtifacts(CAMEL_CORE_MAVEN_ARTIFACT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!ignoreCamelCoreLib) {
            PsiTestUtil.addLibrary(myFixture.getProjectDisposable(), myFixture.getModule(), "Maven: " + CAMEL_CORE_MAVEN_ARTIFACT, mavenArtifacts[0].getParent(), mavenArtifacts[0].getName());
        }
        ApplicationManager
            .getApplication()
            .executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {
                disposeOnTearDown(ServiceManager.getService(getModule().getProject(), CamelCatalogService.class));
                disposeOnTearDown(ServiceManager.getService(getModule().getProject(), CamelService.class));
                disposeOnTearDown(ServiceManager.getService(CamelPreferenceService.class));
                disposeOnTearDown(ServiceManager.getService(CamelIdeaUtils.class));
                disposeOnTearDown(ServiceManager.getService(IdeaUtils.class));
                disposeOnTearDown(ServiceManager.getService(JavaMethodUtils.class));
            }));

        ServiceManager.getService(getModule().getProject(), CamelService.class).setCamelPresent(true);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }

    /**
     * Get a list of artifact declared as dependencies in the pom.xml file.
     * <p>
     *   The method take a String arrays off "G:A:P:C:?" "org.apache.camel:camel-core:2.22.0"
     * </p>
     * @param mavenArtifact - Array of maven artifact to resolve
     * @return Array of artifact files
     * @throws IOException
     */
    protected static File[] getMavenArtifacts(String... mavenArtifact) throws IOException {
        File[] libs = Maven.configureResolver()
            .withRemoteRepo("snapshot", "https://repository.apache.org/snapshots/", "default")
            .resolve(mavenArtifact)
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
        service.setDownloadCatalog(true);
        service.setHighlightCustomOptions(true);

        List<String> expectedIgnoredProperties = Arrays.asList("java.", "Logger.", "logger", "appender.", "rootLogger.",
            "camel.springboot.", "camel.component.", "camel.dataformat.", "camel.language.");
        service.setIgnorePropertyList(expectedIgnoredProperties);
        service.setShowCamelIconInGutter(true);
        service.setScanThirdPartyComponents(true);
        service.setRealTimeSimpleValidation(true);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        LanguageLevel languageLevel = LanguageLevel.JDK_11;
        return new DefaultLightProjectDescriptor() {
            @Override
            public Sdk getSdk() {
                String compilerOption = JpsJavaSdkType.complianceOption(languageLevel.toJavaVersion());
                return JavaSdk.getInstance().createJdk( "java " + compilerOption, BUILD_MOCK_JDK_DIRECTORY + compilerOption, false );
            }

            @Override
            public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
                model.getModuleExtension( LanguageLevelModuleExtension.class ).setLanguageLevel( languageLevel );
            }
        };
    }
}
