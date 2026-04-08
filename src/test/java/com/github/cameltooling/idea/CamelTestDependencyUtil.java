package com.github.cameltooling.idea;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.PsiTestUtil;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

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
    public static File[] getMavenArtifacts(String... mavenArtifact) {
        return downloadArtifacts(false, mavenArtifact);
    }

    public static File[] getMavenArtifactsWithTransitively(String... mavenArtifact) {
        return downloadArtifacts(true, mavenArtifact);
    }

    private static File[] downloadArtifacts(boolean transitively, String... mavenArtifact){
        try (MavenDownloaderImpl impl = new MavenDownloaderImpl()) {
            impl.setMavenApacheSnapshotEnabled(true);
            impl.start();
            var files = impl.resolveArtifacts(List.of(mavenArtifact), null, transitively, true)
                    .stream().map(MavenArtifact::getFile);
            return files.toList().toArray(new File[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
