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

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Supplier;

import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.completion.SimpleSuggestion;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.service.extension.idea.YamlIdeaUtils;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.github.cameltooling.idea.util.StringUtils.fromKebabToCamelCase;

/**
 * The {@link CompletionProvider} that gives the name of the options of main, components, languages and data formats
 * for yaml files.
 */
public class CamelYamlPropertyKeyCompletion extends CamelPropertyKeyCompletion {

    /**
     * Indicates whether the given criteria fulfill the requirements to enable the completion.
     *
     * @param project the project for which the completion is expected.
     * @param file the file for which the completion is expected.
     * @return {@code true} if the requirements are fulfilled such that the completion is enabled, {@code false} otherwise
     */
    static boolean enableCompletion(final Project project, final PsiFile file) {
        return project.getService(CamelService.class).isCamelPresent() && isCamelRuntimeCompatible(project)
            && !ApplicationManager.getApplication().getService(CamelIdeaUtils.class).isCamelFile(file);
    }

    @Override
    protected boolean isEnabled(final Project project, final PsiFile file) {
        return enableCompletion(project, file);
    }

    @Override
    protected @NotNull PsiElement getCompletionPosition(@NotNull CompletionParameters parameters) {
        return parameters.getPosition();
    }

    @Override
    protected String getFullKey(@NotNull PsiElement element) {
        final Deque<String> keys = new LinkedList<>();
        keys.add(getPrefix(element));
        YAMLKeyValue parent = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        while (parent != null) {
            keys.addFirst(parent.getKeyText());
            parent = PsiTreeUtil.getParentOfType(parent, YAMLKeyValue.class);
        }
        return String.join(".", keys);
    }
    @Override
    protected String getPrefix(@NotNull PsiElement element) {
        final String text = element.getText().trim();
        return text.replace(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED, "");
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilderForPrefixSuggestion(final SuggestionContext context,
                                                                                 final String suggestion) {
        final String subpartOfSuggestion = getCurrentSubpartOfSuggestion(context, suggestion);
        if (subpartOfSuggestion == null) {
            // The suggestion doesn't match, so it is replaced by an empty proposal
            return LookupElementBuilder.create("");
        }
        return LookupElementBuilder.create(String.format("%s:%n", subpartOfSuggestion))
            .withLookupString(subpartOfSuggestion)
            .withPresentableText(subpartOfSuggestion)
            .withInsertHandler((ctx, item) -> indentSuggestion(ctx));
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilderForPrefixSuggestion(final SuggestionContext context,
                                                                                 final String suggestion,
                                                                                 final Supplier<String> descriptionSupplier) {
        final String subpartOfSuggestion = getCurrentSubpartOfSuggestion(context, suggestion);
        if (subpartOfSuggestion == null) {
            // The suggestion doesn't match, so it is replaced by an empty proposal
            return LookupElementBuilder.create("");
        } else if (suggestion.endsWith(subpartOfSuggestion)) {
            // The current subpart of the suggestion is the last part of the key, so the description can be supplied
            return LookupElementBuilder.create(
                new SimpleSuggestion(suggestion, descriptionSupplier, String.format("%s:%n", subpartOfSuggestion))
            )
                .withLookupString(subpartOfSuggestion)
                .withPresentableText(subpartOfSuggestion)
                .withInsertHandler((ctx, item) -> indentSuggestion(ctx));
        }
        // The current subpart of the suggestion is an intermediate part of the key, so no description is supplied
        return LookupElementBuilder.create(String.format("%s:%n", subpartOfSuggestion))
            .withLookupString(subpartOfSuggestion)
            .withPresentableText(subpartOfSuggestion)
            .withInsertHandler((ctx, item) -> indentSuggestion(ctx));
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilderForOptionNameSuggestion(final SuggestionContext context,
                                                                                     final BaseOptionModel option,
                                                                                     final String suggestion,
                                                                                     final String suggestionInKebabCase) {
        final String subpartOfSuggestion = getCurrentSubpartOfSuggestion(context, suggestion);
        final String subpartOfSuggestionInKebabCase = getCurrentSubpartOfSuggestion(context, suggestionInKebabCase);
        if (subpartOfSuggestion == null || subpartOfSuggestionInKebabCase == null) {
            // The suggestion doesn't match, so it is replaced by an empty proposal
            return LookupElementBuilder.create("");
        } else if (suggestion.endsWith(subpartOfSuggestion)) {
            // The current subpart of the suggestion is the last part of the key, so the description can be supplied
            return LookupElementBuilder.create(
                new OptionSuggestion(option, String.format("%s: ", subpartOfSuggestionInKebabCase))
            )
                .withLookupString(subpartOfSuggestion)
                .withLookupString(subpartOfSuggestionInKebabCase)
                .withPresentableText(subpartOfSuggestionInKebabCase);
        }
        // The current subpart of the suggestion is an intermediate part of the key, so no description is supplied
        return LookupElementBuilder.create(String.format("%s:%n", subpartOfSuggestionInKebabCase))
            .withLookupString(subpartOfSuggestion)
            .withLookupString(subpartOfSuggestionInKebabCase)
            .withPresentableText(subpartOfSuggestionInKebabCase)
            .withInsertHandler((ctx, item) -> indentSuggestion(ctx));
    }

    /**
     * Gives the subpart of the given suggestion corresponding to the given context.
     * @param context the context from which the current value of the full key is extracted
     * @param suggestion the suggestion from which the subpart is extracted.
     * @return the sub part of the suggestion that matches with the content of the current full key, {@code null} if it
     * doesn't match (not same root or subpart out of scope)
     */
    private @Nullable String getCurrentSubpartOfSuggestion(final SuggestionContext context, final String suggestion) {
        final String fullKey = context.getFullKey();
        // Check if the suggestion starts with the current full key in camel case
        if (fromKebabToCamelCase(suggestion).startsWith(fromKebabToCamelCase(fullKey))) {
            // Get the total amount of levels in the current full key corresponding to the amount of dots
            final int index = (int) fullKey.chars().filter(c -> c == '.').count();
            final String[] strings = suggestion.split("\\.");
            return index < strings.length ? strings[index] : null;
        }
        return null;
    }

    /**
     * Indicates whether the Camel Runtime of the given project is compatible with the code completion on Yaml files.
     * @param project the project to test.
     * @return {@code true} if the Camel Runtime of the given project is compatible, {@code false} otherwise.
     */
    private static boolean isCamelRuntimeCompatible(final Project project) {
        final CamelCatalogProvider provider = ApplicationManager.getApplication()
            .getService(CamelPreferenceService.class)
            .getCamelCatalogProvider()
            .getActualProvider(project);
        return provider == CamelCatalogProvider.QUARKUS || provider == CamelCatalogProvider.SPRING_BOOT;
    }

    /**
     * Indent automatically the suggestion using the indent of the previous line to which one more indent it added.
     * @param context the context of the insertion
     */
    private static void indentSuggestion(InsertionContext context) {
        context.commitDocument();
        // Adjust the indent for the next input content
        context.getDocument().insertString(
            context.getTailOffset(),
            YamlIdeaUtils.getIndent(context.getEditor(), context.getStartOffset(), 1)
        );
    }
}
