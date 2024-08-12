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

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;

/**
 * {@code CamelKameletCompletion} is a base class for all Camel Kamelet completion providers.
 */
abstract class CamelKameletCompletion extends CompletionProvider<CompletionParameters> {

    protected final CamelEndpointSmartCompletionExtension.Mode mode = CamelEndpointSmartCompletionExtension.Mode.KAMELET;

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        Project project = parameters.getOriginalFile().getProject();
        if (project.getService(CamelService.class).isCamelProject()) {
            final CamelCatalog camelCatalog = project.getService(CamelCatalogService.class).get();
            final IdeaUtils ideaUtils = IdeaUtils.getService();
            PsiElement element = parameters.getPosition();
            String val = ideaUtils.extractTextFromElement(element, true, true, true);
            if (val == null || val.isEmpty()) {
                return;
            }
            val = val.replace(CompletionUtilCore.DUMMY_IDENTIFIER, "")
                .replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "");
            final ComponentModel componentModel = mode.componentModel(
                project, camelCatalog, "kamelet", getURIPrefix(element) + val, isConsumerOnly(project, element)
            );
            if (componentModel == null) {
                return;
            }
            final List<LookupElement> answer = suggestions(componentModel, project, element, parameters.getEditor(), val);
            // are there any results then add them
            if (!answer.isEmpty()) {
                result.withPrefixMatcher(val).addAllElements(answer);
                result.stopHere();
            }
        }
    }

    /**
     * @return the URI prefix to use when loading the component model.
     */
    @NotNull
    protected abstract String getURIPrefix(PsiElement element);

    /**
     * @param project the project to check.
     * @param element the element corresponding to the position of the completion.
     * @return {@code true} if the component is consumer only, {@code false} otherwise.
     */
    protected abstract boolean isConsumerOnly(Project project, PsiElement element);

    /**
     * @return the list of suggestions for the given input value.
     */
    protected abstract List<LookupElement> suggestions(@NotNull ComponentModel componentModel, @NotNull Project project,
                                                       @NotNull PsiElement element, @NotNull Editor editor, String val);
}
