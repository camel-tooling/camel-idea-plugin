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
package com.github.cameltooling.idea.completion.extension;

import java.util.List;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Code completion for camel endpoint names (only for direct endpoints so far).
 */
public class CamelEndpointNameCompletionExtension extends SimpleCompletionExtension {

    @Override
    protected List<LookupElement> findResults(@NotNull CompletionParameters parameters, @NotNull PsiElement element, @NotNull String query) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        List<PsiElement> endpointDeclarations = CamelIdeaUtils.getService().findEndpointDeclarations(module, e -> true);
        return endpointDeclarations.stream()
            .map(this::createLookupElement)
            .collect(Collectors.toList());
    }

    @NotNull
    private LookupElement createLookupElement(PsiElement endpointElement) {
        CamelEndpoint endpoint = new CamelEndpoint(endpointElement.getText());
        return createLookupElement(endpoint.getBaseUri(), endpointElement);
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull String query) {
        if (query.contains("?")) {
            //do not add completions when inside endpoint query params
            return false;
        } else if (query.contains(CamelIdeaUtils.PROPERTY_PLACEHOLDER_START_TAG)) {
            //do not add completions when endpoint contains property placeholders
            return false;
        }
        PsiElement element = parameters.getPosition();
        if (element instanceof PsiJavaToken) {
            element = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression.class);
            if (element == null) {
                return false;
            }
        }
        CamelIdeaUtils service = CamelIdeaUtils.getService();
        return service.isPlaceForEndpointUri(element) && service.isProducerEndpoint(element);
    }

}
