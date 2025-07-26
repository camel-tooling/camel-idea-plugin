package com.github.cameltooling.idea.gutter;

import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GutterPsiTargetPresentationRenderer extends PsiTargetPresentationRenderer<PsiElement> {

    @Override
    @NotNull
    public String getElementText(@NotNull PsiElement element) {
        String text = IdeaUtils.getService().extractTextFromElement(element, true, false, true);
        if (text == null) {
            text = element.getText();
        }
        return StringUtil.unquoteString(text);
    }

    @Override
    public @Nls @Nullable String getContainerText(@NotNull PsiElement element) {
        int lineNumber = IdeaUtils.getLineNumber(element);
        return element.getContainingFile().getVirtualFile().getName() + ":" + (lineNumber + 1);
    }

    @Override
    protected Icon getIcon(PsiElement psiElement) {
        return psiElement.getContainingFile().getIcon(Iconable.ICON_FLAG_VISIBILITY);
    }

}
