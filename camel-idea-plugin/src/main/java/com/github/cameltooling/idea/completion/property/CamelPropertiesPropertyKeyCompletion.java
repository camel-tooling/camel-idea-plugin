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
package com.github.cameltooling.idea.completion.property;

import java.util.function.Supplier;

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.completion.SimpleSuggestion;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link CompletionProvider} that gives the name of the options of main, components, languages and data formats
 * for properties files.
 */
public class CamelPropertiesPropertyKeyCompletion extends CamelPropertyKeyCompletion {

    @Override
    protected @NotNull PsiElement getCompletionPosition(@NotNull CompletionParameters parameters) {
        final PsiElement element = parameters.getOriginalPosition();
        return element == null ? parameters.getPosition() : element;
    }

    @Override
    protected boolean isEnabled(Project project, PsiFile file) {
        return project.getService(CamelService.class).isCamelPresent();
    }

    @Override
    protected String getFullKey(final @NotNull PsiElement element) {
        final String result = element.getText().trim();
        return CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.equals(result) ? "" : result;
    }

    @Override
    protected String getPrefix(@NotNull PsiElement element) {
        return getFullKey(element);
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilderForPrefixSuggestion(final SuggestionContext context,
                                                                                 final String suggestion) {
        return LookupElementBuilder.create(String.format("%s.", suggestion))
            .withLookupString(suggestion)
            .withPresentableText(suggestion);
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilderForPrefixSuggestion(final SuggestionContext context,
                                                                                 final String suggestion,
                                                                                 final Supplier<String> descriptionSupplier) {
        return LookupElementBuilder.create(new SimpleSuggestion(suggestion, descriptionSupplier, String.format("%s.", suggestion)))
            .withLookupString(suggestion)
            .withPresentableText(suggestion);
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilderForOptionNameSuggestion(final SuggestionContext context,
                                                                                     final BaseOptionModel option,
                                                                                     final String suggestion,
                                                                                     final String suggestionInKebabCase) {
        return LookupElementBuilder.create(
                new OptionSuggestion(option, String.format("%s = ", suggestionInKebabCase))
            )
            .withLookupString(suggestionInKebabCase)
            .withLookupString(suggestion)
            .withPresentableText(suggestionInKebabCase);
    }
}
