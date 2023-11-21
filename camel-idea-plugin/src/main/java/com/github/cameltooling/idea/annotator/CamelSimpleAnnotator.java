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

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.LanguageValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Validate simple expression and annotated the specific simple expression to highlight the error in the editor
 */
public class CamelSimpleAnnotator extends AbstractCamelAnnotator {

    private static final Logger LOG = Logger.getInstance(CamelEndpointAnnotator.class);

    @Override
    boolean isEnabled() {
        return CamelPreferenceService.getService().isRealTimeSimpleValidation();
    }

    /**
     * Validate simple expression. eg simple("${body}")
     * if the expression is not valid a error annotation is created and highlight the invalid value.
     */
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text) {

        final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();

        // we only want to evaluate if there is a simple function as plain text without functions dont make sense to validate
        boolean hasSimple = text.contains("${") || text.contains("$simple{");
        if (hasSimple && camelIdeaUtils.isCamelExpression(element, "simple")) {
            CamelCatalog catalogService = element.getProject().getService(CamelCatalogService.class).get();
            CamelService camelService = element.getProject().getService(CamelService.class);

            boolean predicate = false;
            try {
                // need to use the classloader that can load classes from the camel-core
                ClassLoader loader = camelService.getCamelCoreClassloader();
                if (loader != null) {
                    LanguageValidationResult result;
                    predicate = camelIdeaUtils.isCamelExpressionUsedAsPredicate(element, "simple");
                    if (predicate) {
                        LOG.debug("Validate simple predicate: " + text);
                        result = catalogService.validateLanguagePredicate(loader, "simple", text);
                    } else {
                        LOG.debug("Validate simple expression: " + text);
                        result = catalogService.validateLanguageExpression(loader, "simple", text);
                    }
                    if (!result.isSuccess()) {
                        String error = result.getShortError();
                        if ("[null]".equals(error)) {
                            return;
                        }
                        TextRange range = element.getTextRange();
                        if (result.getIndex() > 0) {
                            range = getAdjustedTextRange(element, range, text, result);
                        }
                        holder.newAnnotation(HighlightSeverity.ERROR, error)
                                .range(range).create();
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Error validating Camel simple " + (predicate ? "predicate" : "expression") + ": " + text, e);
            }
        }
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
        //we need to calculate the correct start and end position to be sure we highlight the correct word
        int startIdx = result.getIndex();
        //test if the simple expression is closed correctly
        int endIdx = text.indexOf("}", startIdx);
        if (endIdx == -1) {
            //the expression is not closed, test for first " " to see if can stop text range here
            endIdx = text.indexOf(" ", startIdx) - 1;
        }
        //calc the end index for highlighted word
        endIdx = endIdx < 0 ? range.getEndOffset() : (range.getStartOffset() + endIdx) + 1;

        if (endIdx < startIdx) {
            endIdx = range.getEndOffset();
        }
        range = TextRange.create(range.getStartOffset() + result.getIndex(), endIdx);
        return range;
    }

}
