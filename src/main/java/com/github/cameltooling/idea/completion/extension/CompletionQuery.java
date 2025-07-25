package com.github.cameltooling.idea.completion.extension;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.psi.PsiElement;

import static com.github.cameltooling.idea.util.CamelIdeaUtils.PROPERTY_PLACEHOLDER_START_TAG;

public record CompletionQuery(PsiElement element, String value, String suffix, String valueAtPosition) {

    public boolean isInsidePropertyPlaceholder() { //TODO: move to CamelIdeaUtils
        int startIndex = valueAtPosition.lastIndexOf(CamelIdeaUtils.PROPERTY_PLACEHOLDER_START_TAG);
        int endIndex = valueAtPosition.lastIndexOf(CamelIdeaUtils.PROPERTY_PLACEHOLDER_END_TAG);
        return startIndex >= 0 && endIndex < startIndex;
    }

    public boolean isClosingPropertyPlaceholderInSuffix() {
        int startTagIndex = suffix.indexOf(PROPERTY_PLACEHOLDER_START_TAG);
        int endTagIndex = suffix.indexOf(CamelIdeaUtils.PROPERTY_PLACEHOLDER_END_TAG);
        return endTagIndex >= 0 && (startTagIndex < 0 || startTagIndex > endTagIndex);
    }

}
