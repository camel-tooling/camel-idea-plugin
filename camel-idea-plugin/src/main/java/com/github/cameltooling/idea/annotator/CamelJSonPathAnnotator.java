/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.idea.annotator;

import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.LanguageValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Validate JSonPath expression and annotated the specific jsonpath expression to highlight the error in the editor
 */
public class CamelJSonPathAnnotator extends AbstractCamelAnnotator {

    private static final Logger LOG = Logger.getInstance(CamelEndpointAnnotator.class);
    private CamelIdeaUtilsExtension camelIdeaUtils;

    @Override
    boolean isEnabled() {
        return CamelPreferenceService.getService().isRealTimeJSonPathValidation();
    }

    /**
     * Validate jsonpath expression. eg jsonpath("$.store.book[?(@.price < 10)]")
     * if the expression is not valid a error annotation is created and highlight the invalid value.
     */
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text) {
        final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();
        // only validate if the element is jsonpath element
        if (camelIdeaUtils.isCamelExpression(element, "jsonpath")) {
            validateJsonpathExpression(element, holder, text);
        }
    }

    void validateJsonpathExpression(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text) {
        Project project = element.getProject();
        CamelCatalog catalogService = project.getService(CamelCatalogService.class).get();
        CamelService camelService = project.getService(CamelService.class);

        // must have camel-json library
        boolean jsonLib = camelService.containsLibrary("camel-jsonpath", false);
        if (!jsonLib) {
            camelService.showMissingJSonPathJarNotification();
            return;
        }

        try {
            // need to use the classloader that can load classes from the project
            ClassLoader loader = camelService.getProjectClassloader();
            if (loader != null) {
                LanguageValidationResult result;
                boolean predicate = camelIdeaUtils.isCamelExpressionUsedAsPredicate(element, "jsonpath");
                if (predicate) {
                    LOG.debug("Inspecting jsonpath predicate: " + text);
                    result = catalogService.validateLanguagePredicate(loader, "jsonpath", text);
                } else {
                    LOG.debug("Inspecting jsonpath expression: " + text);
                    result = catalogService.validateLanguageExpression(loader, "jsonpath", text);
                }
                if (!result.isSuccess()) {
                    handleJsonpathValidationFailure(element, holder, result, text);
                }
            }
        } catch (Throwable e) {
            LOG.warn("Error inspecting Camel jsonpath: " + text, e);
        }
    }

    void handleJsonpathValidationFailure(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull LanguageValidationResult result, @NotNull String text) {
        String error = result.getShortError();
        if ("[null]".equals(error)) {
            return;
        }
        if (error == null) {
            error = result.getError();
        }
        TextRange range = element.getTextRange();
        if (result.getIndex() > 0) {
            range = getAdjustedTextRange(element, range, text, result);
        }
        holder.newAnnotation(HighlightSeverity.ERROR, error)
                .range(range).create();
    }
    /**
     * Adjust the text range according to the type of ${@link PsiElement}
     * @return a new text range
     */

    private TextRange getAdjustedTextRange(@NotNull PsiElement element, TextRange range, String text, LanguageValidationResult result) {
        if (element instanceof XmlAttributeValue) {
            // we can use the xml range as-is
            range = ((XmlAttributeValue) element).getValueTextRange();
        } else if (IdeaUtils.getService().isJavaLanguage(element)) {
            // all the programming languages need to have the offset adjusted by 1
            range = TextRange.create(range.getStartOffset() + 1, range.getEndOffset());
        }

        // calculate the correct start and end position to be sure we highlight the correct word
        int startIdx = result.getIndex();
        int endIdx = calculateEndIndex(startIdx, text);

        // calculate the end index for highlighted word
        endIdx = calculateEndOffset(endIdx, range);

        if (endIdx <= startIdx) {
            endIdx = range.getEndOffset();
        }

        range = TextRange.create(range.getStartOffset() + result.getIndex(), endIdx);
        return range;
    }

    private int calculateEndIndex(int startIdx, String text) {
        int endIdx = text.indexOf("}", startIdx);
        if (endIdx == -1) {
            endIdx = text.indexOf(" ", startIdx) - 1;
        }
        return endIdx < 0 ? Integer.MAX_VALUE : endIdx;
    }

    private int calculateEndOffset(int endIdx, TextRange range) {
        return endIdx < Integer.MAX_VALUE ? range.getStartOffset() + endIdx + 1 : range.getEndOffset() - 1;
    }
}
