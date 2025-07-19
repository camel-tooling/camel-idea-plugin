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
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.JavaMethodUtils;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Super class for Camel Plugin Testing. If you are testing plug-in code with LightCodeInsightFixtureTestCase
 * you should extend this class to make sure it is setup as expected and clean up on tearDown
 */
public abstract class CamelLightCodeInsightFixtureTestCaseIT extends LightJavaCodeInsightFixtureTestCase {

    private static final File[] mavenArtifacts;
    private static final String TEST_DATA_BASE = "src/test/resources/testData/";
    private boolean ignoreCamelCoreLib;

    protected static String CAMEL_VERSION;
    protected static String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:%s";
    protected static String CAMEL_CORE_MODEL_MAVEN_ARTIFACT = "org.apache.camel:camel-core-model:%s";


    static {
        final String projectRoot = new File(System.getProperty("user.dir")).getPath();
        try (InputStream is = new FileInputStream(projectRoot +"/gradle.properties")) {
            Properties gradleProperties = new Properties();
            gradleProperties.load(is);
            CAMEL_VERSION = gradleProperties.getProperty("camelVersion");
            CAMEL_CORE_MAVEN_ARTIFACT = String.format(CAMEL_CORE_MAVEN_ARTIFACT, CAMEL_VERSION);
            CAMEL_CORE_MODEL_MAVEN_ARTIFACT = String.format(CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_VERSION);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mavenArtifacts = getMavenArtifacts(CAMEL_CORE_MAVEN_ARTIFACT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!ignoreCamelCoreLib) {
            PsiTestUtil.addLibrary(myFixture.getProjectDisposable(), myFixture.getModule(), "Maven: " + CAMEL_CORE_MAVEN_ARTIFACT, mavenArtifacts[0].getParent(), mavenArtifacts[0].getName());
        }
        Project project = getModule().getProject();
        Application application = ApplicationManager.getApplication();
        application
            .executeOnPooledThread(() -> application.runReadAction(() -> {
                disposeOnTearDown(project.getService(CamelCatalogService.class));
                disposeOnTearDown(project.getService(CamelService.class));
                disposeOnTearDown(application.getService(CamelPreferenceService.class));
                disposeOnTearDown(application.getService(CamelIdeaUtils.class));
                disposeOnTearDown(application.getService(IdeaUtils.class));
                disposeOnTearDown(application.getService(JavaMethodUtils.class));
            }));

        project.getService(CamelService.class).setCamelPresent(true);

        allowTestDataAccess();
    }

    private void allowTestDataAccess() {
        Path path = Paths.get(TEST_DATA_BASE);
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), path.toAbsolutePath().toString());
    }

    @Override
    protected String getTestDataPath() {
        return TEST_DATA_BASE;
    }

    /**
     * Get a list of artifact declared as dependencies in the pom.xml file.
     * <p>
     *   The method take a String arrays off "G:A:P:C:?" "org.apache.camel:camel-core:2.22.0"
     * </p>
     * @param mavenArtifact - Array of maven artifact to resolve
     * @return Array of artifact files
     */
    protected static File[] getMavenArtifacts(String... mavenArtifact) {
        return Maven.configureResolver()
            .withRemoteRepo("snapshot", "https://repository.apache.org/snapshots/", "default")
            .resolve(mavenArtifact)
            .withoutTransitivity().asFile();
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

    /**
     * @return an array of the dependencies to add to the module in the following format
     * {@code group-id:artifact-id:version}. No additional dependencies by default.
     */
    @Nullable
    protected String[] getMavenDependencies() {
        return null;
    }

    protected void initCamelPreferencesService() {
        List<String> expectedExcludedProperties = Arrays.asList("**/log4j.properties", "**/log4j2.properties", "**/logging.properties");
        CamelPreferenceService service = ApplicationManager.getApplication().getService(CamelPreferenceService.class);
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
        return new DefaultLightProjectDescriptor() {
            @Override
            public Sdk getSdk() {
                return IdeaTestUtil.getMockJdk17();
            }

            @Override
            public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
                loadDependencies(model);
                model.getModuleExtension( LanguageLevelModuleExtension.class ).setLanguageLevel( LanguageLevel.JDK_11 );
            }
        };
    }

    /**
     * Loads the maven dependencies into the given model.
     * @param model the model into which the dependencies are added.
     */
    protected void loadDependencies(@NotNull ModifiableRootModel model) {
        String[] dependencies = getMavenDependencies();
        if (dependencies != null && dependencies.length > 0) {
            File[] artifacts = getMavenArtifacts(dependencies);
            for (int i = 0; i < artifacts.length; i++) {
                File artifact = artifacts[i];
                PsiTestUtil.addLibrary(model, dependencies[i], artifact.getParent(), artifact.getName());
            }
        }
    }
}
