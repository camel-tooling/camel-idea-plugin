
package com.github.cameltooling.idea.inspection;


import java.io.File;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.JavaInspectionTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Test helper for setup test case to test inspection code. The class create a new {@link LightProjectDescriptor} for
 * each test to make sure it starts with a clean state and all previous added libraries is removed
 *
 */
public abstract class CamelInspectionTestHelper extends JavaInspectionTestCase {

    public static final String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:3.20.0";

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
     * @return an array of the dependencies to add to the module in the following format
     * {@code group-id:artifact-id:version}. No additional dependencies by default.
     */
    @Nullable
    protected String[] getMavenDependencies() {
        return null;
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

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/";
    }
}

