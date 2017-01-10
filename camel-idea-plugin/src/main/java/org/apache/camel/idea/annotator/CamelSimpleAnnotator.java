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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.SimpleValidationResult;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Validate simple expression and annotated the specific simple expression to highlight the error in the editor
 */
public class CamelSimpleAnnotator extends AbstractCamelAnnotator {

    @Override
    boolean isEnabled() {
        return ServiceManager.getService(CamelPreferenceService.class).isRealTimeSimpleValidation();
    }

    /**
     * Validate simple expression. eg simple("${body}")
     * if the expression is not valid a error annotation is created and highlight the invalid value.
     */
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text) {
        boolean hasSimple = text.contains("${") || text.contains("$simple{");
        if (hasSimple && IdeaUtils.isCamelRouteSimpleExpression(element)) {
            CamelCatalog catalogService = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get();
            CamelService camelService = ServiceManager.getService(element.getProject(), CamelService.class);

            try {
                // need to use the classloader that can load classes from the camel-core
                ClassLoader loader = camelService.getCamelCoreClassloader();
                if (loader != null) {
                    SimpleValidationResult result = catalogService.validateSimpleExpression(loader, text);
                    if (!result.isSuccess()) {
                        String error = result.getError();
                        int propertyIdx = text.indexOf("simple");
                        int propertyLength = text.length() + 8;
                        TextRange range = new TextRange(element.getTextRange().getStartOffset() + propertyIdx,
                            element.getTextRange().getStartOffset() + propertyIdx + propertyLength);
                        holder.createErrorAnnotation(range, error);
                    }
                }
            } catch (Throwable e) {
                // ignore
            }
        }
    }

}
