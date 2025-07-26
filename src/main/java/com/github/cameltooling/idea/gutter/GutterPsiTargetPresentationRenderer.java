package com.github.cameltooling.idea.gutter;

import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GutterPsiTargetPresentationRenderer extends PsiTargetPresentationRenderer<PsiElement> {

    @Override
    @NotNull
    public String getElementText(@NotNull PsiElement element) {
        if (element instanceof PsiLiteralExpression ple) {
            Object value = ple.getValue();
            if (value != null) {
                return value.toString();
            }
        }
        return element.getText();
    }

    @Override
    public @Nls @Nullable String getContainerText(@NotNull PsiElement element) {
        return element.getContainingFile().getVirtualFile().getName();
    }

    @Override
    protected Icon getIcon(PsiElement psiElement) {
        return psiElement.getContainingFile().getIcon(Iconable.ICON_FLAG_VISIBILITY);
    }

}
