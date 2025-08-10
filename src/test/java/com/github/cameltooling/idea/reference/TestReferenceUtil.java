package com.github.cameltooling.idea.reference;

import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.junit.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
public final class TestReferenceUtil {

    private TestReferenceUtil() {
    }

    public static PsiElement getParentElementAtCaret(JavaCodeInsightTestFixture fixture) {
        PsiElement element = fixture.getFile().findElementAt(fixture.getCaretOffset());
        Assert.assertNotNull(element);
        return element.getParent();
    }

    public static <T extends PsiElement> List<T> resolveReference(PsiElement element, Class<T> targetClass) {
        return resolveReference(element).stream()
            .map(e -> PsiTreeUtil.getParentOfType(e, targetClass, false))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<PsiElement> resolveReference(PsiElement element) {
        PsiReference[] references = element.getReferences();
        Assert.assertEquals(1, references.length);
        PsiReference reference = references[0];
        if (reference instanceof PsiPolyVariantReference) {
            ResolveResult[] results = ((PsiPolyVariantReference) reference).multiResolve(false);
            return Arrays.stream(results)
                .map(ResolveResult::getElement)
                .filter(Objects::nonNull)
                .map(TestReferenceUtil::getPsiElement)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } else {
            return Stream.of(getPsiElement(reference.resolve()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    private static PsiElement getPsiElement(PsiElement element) {
        if (element instanceof FakeCamelPsiElement) {
            return ((FakeCamelPsiElement) element).getNavigationElement();
        } else {
            return element;
        }
    }

    public static List<PropertyReference> getPropertyReferences(PsiElement caretElement) {
        return getReferencesOfType(caretElement, PropertyReference.class);
    }

    public static <T extends PsiReference>  List<T> getReferencesOfType(PsiElement caretElement, Class<T> refType) {
        PsiReference[] refs = caretElement.getReferences();
        return Arrays.stream(refs)
                .filter(refType::isInstance)
                .map(refType::cast)
                .toList();
    }

}
