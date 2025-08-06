
package com.github.cameltooling.idea.inspection;


import java.io.File;

import com.github.cameltooling.idea.CamelTestDependencyUtil;
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
                CamelTestDependencyUtil.loadDependencies(model, getMavenDependencies());
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

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/";
    }
}

