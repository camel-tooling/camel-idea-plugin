package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.CamelTestDependencyUtil;
import com.github.cameltooling.idea.reference.endpoint.direct.DirectEndpointReference;
import com.github.cameltooling.idea.reference.endpoint.direct.DirectEndpointStartSelfReference;
import com.intellij.codeInsight.JavaCodeInsightTestCase;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.IndexingTestUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

@RunWith(JUnit4.class)
public class CamelDirectEndpointReferencesBetweenModulesIT extends JavaCodeInsightTestCase {

    private Module moduleA;
    private Module moduleB;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Path path = Paths.get(getTestDataPath());
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), path.toAbsolutePath().toString());

        moduleA = initModule("module-a");
        moduleB = initModule("module-b");
    }

    private Module initModule(String name) throws IOException {
        return WriteAction.computeAndWait(() -> {
            Module module = createModuleFromTestData(getTestDataPath() + name + "/", name + ".iml", JavaModuleType.getModuleType(), true);

            ModuleRootModificationUtil.updateModel(module, modifiableRootModel -> {
                modifiableRootModel.setSdk(IdeaTestUtil.getMockJdk21());
                modifiableRootModel.getModuleExtension(LanguageLevelModuleExtension.class).setLanguageLevel(LanguageLevel.JDK_17);

                CamelTestDependencyUtil.loadDependencies(modifiableRootModel, "org.apache.camel:camel-core-model:4.13.0");
            });
            IndexingTestUtil.waitUntilIndexesAreReady(module.getProject());

            return module;
        });
    }

    @Override
    protected @NotNull String getTestDataPath() {
        return "src/test/resources/testData/reference/multi-module/";
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return IdeaTestUtil.getMockJdk21();
    }

    @Test
    public void findFromEndpointInModuleDependency() throws Exception {
        ModuleRootModificationUtil.updateModel(moduleA, modifiableRootModel -> {
            modifiableRootModel.addModuleOrderEntry(moduleB);
        });

        configureByModuleFile(moduleB, "src/RouteB.java");
        PsiElement fromRouteBElement = findElementAtText("direct:routeB");
        assertHasReference(fromRouteBElement, DirectEndpointStartSelfReference.class);

        configureByModuleFile(moduleA, "src/RouteA.java");
        PsiElement toRouteBElement = findElementAtText("direct:routeB");
        DirectEndpointReference toRef = assertHasReference(toRouteBElement, DirectEndpointReference.class);
        PsiElement target = toRef.resolve();
        assertNotNull(target);
        assertEquals(fromRouteBElement, target.getNavigationElement());
    }

    @Test
    public void fromEndpointNotFoundIfNotInModuleDependency() throws Exception {
        configureByModuleFile(moduleA, "src/RouteA.java");
        PsiElement toRouteBElement = findElementAtText("direct:routeB");
        DirectEndpointReference toRef = assertHasReference(toRouteBElement, DirectEndpointReference.class);
        assertNull(toRef.resolve());
    }

    private void configureByModuleFile(Module moduleB, String relPath) throws IOException {
        configureByExistingFile(Objects.requireNonNull(getOrCreateModuleDir(moduleB).findFileByRelativePath(relPath)));
    }

    private PsiLiteralExpression findElementAtText(String elementText) {
        PsiLiteralExpression element = PsiTreeUtil.getParentOfType(getFile().findElementAt(getFile().getText().indexOf(elementText)), PsiLiteralExpression.class);
        assertNotNull(element);
        return element;
    }

    private <T extends PsiReference> T assertHasReference(PsiElement element, Class<T> referenceClass) {
        T reference = Arrays.stream(element.getReferences())
                .filter(referenceClass::isInstance)
                .map(referenceClass::cast)
                .findFirst().orElse(null);
        assertNotNull(reference);
        return reference;
    }

}
