package com.github.cameltooling.idea;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.PsiTestUtil;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CamelTestDependencyUtil {

    /**
     * TODO: there's com.intellij.testFramework.fixtures.MavenDependencyUtil#addFromMaven, but it does not work as well
     *       as this one for some reason - when attached via it, the lib classes are not searchable via psi tree. Find out why.
     * Loads the maven dependencies into the given model.
     * @param model the model into which the dependencies are added.
     */
    public static void loadDependencies(@NotNull ModifiableRootModel model, String ... dependencies) {
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

}
