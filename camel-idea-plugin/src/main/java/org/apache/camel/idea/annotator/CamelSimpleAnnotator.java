/**
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
package org.apache.camel.idea.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.SimpleValidationResult;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Validate simple expression and annotated the specific simple expression to highlight the error in the editor
 */
public class CamelSimpleAnnotator extends AbstractCamelAnnotator {

    private static final Logger LOG = Logger.getInstance(CamelEndpointAnnotator.class);

    @Override
    boolean isEnabled() {
        return ServiceManager.getService(CamelPreferenceService.class).isRealTimeSimpleValidation();
    }

    /**
     * Validate simple expression. eg simple("${body}")
     * if the expression is not valid a error annotation is created and highlight the invalid value.
     */
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text) {

        // we only want to evaluate if there is a simple function as plain text without functions dont make sense to validate
        boolean hasSimple = text.contains("${") || text.contains("$simple{");
        if (hasSimple && getCamelIdeaUtils().isCamelSimpleExpression(element)) {
            CamelCatalog catalogService = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get();
            CamelService camelService = ServiceManager.getService(element.getProject(), CamelService.class);

            boolean predicate = false;
            try {
                // need to use the classloader that can load classes from the camel-core
                ClassLoader loader = camelService.getCamelCoreClassloader();
                if (loader != null) {
                    SimpleValidationResult result;
                    predicate = getCamelIdeaUtils().isCameSimpleExpressionUsedAsPredicate(element);
                    if (predicate) {
                        LOG.debug("Validate simple predicate: " + text);
                        result = catalogService.validateSimplePredicate(loader, text);
                    } else {
                        LOG.debug("Validate simple expression: " + text);
                        result = catalogService.validateSimpleExpression(loader, text);
                    }
                    if (!result.isSuccess()) {
                        String error = result.getShortError();
                        TextRange range = element.getTextRange();
                        if (result.getIndex() > 0) {
                            range = getAdjustedTextRange(element, range, text, result);

                        }
                        holder.createErrorAnnotation(range, error);
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
    private TextRange getAdjustedTextRange(@NotNull PsiElement element, TextRange range, String text, SimpleValidationResult result) {
        if (element instanceof XmlAttributeValue) {
            // we can use the xml range as-is
            range = ((XmlAttributeValue) element).getValueTextRange();
        } else if (getIdeaUtils().isJavaLanguage(element)) {
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
        endIdx = endIdx < 0 ? (range.getEndOffset() - 1) : (range.getStartOffset() + endIdx) + 1;

        if (endIdx <= startIdx) {
            endIdx = range.getEndOffset();
        }
        range = TextRange.create(range.getStartOffset() + result.getIndex(), endIdx);
        return range;
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }
    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

}
