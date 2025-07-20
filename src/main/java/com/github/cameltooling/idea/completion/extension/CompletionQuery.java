package com.github.cameltooling.idea.completion.extension;

import com.intellij.psi.PsiElement;

public record CompletionQuery(PsiElement element, String value, String suffix, String valueAtPosition) {
}
