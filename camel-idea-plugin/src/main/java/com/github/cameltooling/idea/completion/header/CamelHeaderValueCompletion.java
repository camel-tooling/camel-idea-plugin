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
package com.github.cameltooling.idea.completion.header;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.completion.header.CamelHeaderEndpointSource.PRODUCER_ONLY;

/**
 * Base class of all supported {@link CompletionProvider}s that give the potential values that can be proposed
 * according to the chosen header.
 */
abstract class CamelHeaderValueCompletion extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet resultSet) {
        final PsiElement element = getCompletionPosition(parameters);
        if (element == null) {
            return;
        }
        final String headerName = getHeaderName(element);
        if (headerName == null) {
            return;
        }
        final Project project = parameters.getOriginalFile().getManager().getProject();
        final Predicate<ComponentModel.EndpointHeaderModel> predicate = predicate(headerName);
        for (CamelHeaderEndpoint endpoint : PRODUCER_ONLY.getEndpoints(element)) {
            // it is a known Camel component
            final String json = getCamelCatalog(project).componentJSonSchema(endpoint.getComponentName());
            if (json == null) {
                continue;
            }
            final ComponentModel componentModel = JsonMapper.generateComponentModel(json);
            final Optional<? extends ComponentModel.EndpointHeaderModel> result = componentModel.getEndpointHeaders()
                .stream()
                .filter(predicate)
                .findFirst();
            if (result.isPresent()) {
                final List<LookupElement> answer = getSuggestions(element, result.get());
                if (!answer.isEmpty()) {
                    // sort the headers A..Z which is easier to users to understand
                    answer.sort((o1, o2) -> o1
                        .getLookupString()
                        .compareToIgnoreCase(o2.getLookupString()));
                    resultSet.withPrefixMatcher(getPrefix(element, resultSet.getPrefixMatcher().getPrefix()))
                        .addAllElements(answer);
                }
                return;
            }
        }
    }

    /**
     * Gives all the possible suggestions of name of header for the given endpoint.
     * @param element the element into which the value of header should be injected.
     * @param header the header for which we expect value suggestions.
     * @return a list of {@link LookupElement} corresponding to the possible suggestions.
     */
    private List<LookupElement> getSuggestions(final PsiElement element,
                                               final ComponentModel.EndpointHeaderModel header) {
        final List<LookupElement> answer = new ArrayList<>();

        final String javaType = header.getJavaType();
        final boolean deprecated = header.isDeprecated();
        final List<String> enums = header.getEnums();
        final Object defaultValue = header.getDefaultValue();

        if (enums != null) {
            addEnumSuggestions(element, answer, deprecated, enums, defaultValue, javaType);
        } else if ("java.lang.Boolean".equalsIgnoreCase(javaType) || "boolean".equalsIgnoreCase(javaType)) {
            addBooleanSuggestions(element, answer, deprecated, defaultValue);
        } else if (defaultValue != null) {
            // for any other kind of type and if there is a default value then add that as a suggestion
            // so it is easy to see what the default value is
            addDefaultValueSuggestions(element, answer, deprecated, defaultValue);
        }

        return answer;
    }

    /**
     * Adds the possible value suggestions to the given list of {@link LookupElement} in case the value is an
     * enum.
     */
    private void addEnumSuggestions(final PsiElement element, final List<LookupElement> answer,
                                    final boolean deprecated, final List<String> enums,
                                    final Object defaultValue, final String javaType) {
        for (String part : enums) {
            LookupElementBuilder builder = createEnumLookupElementBuilder(element, part, javaType);
            // only show the option in the UI
            builder = builder.withPresentableText(part);
            builder = builder.withBoldness(true);
            if (deprecated) {
                // mark as deprecated
                builder = builder.withStrikeoutness(true);
            }
            boolean isDefaultValue = part.equals(defaultValue);
            if (isDefaultValue) {
                builder = builder.withTailText(" (default value)");
                // add default value first in the list
                answer.add(0, asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)));
            } else {
                answer.add(asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)));
            }
        }
    }

    /**
     * Adds the possible value suggestions to the given list of {@link LookupElement} in case the value is a
     * {@code boolean}.
     */
    private void addBooleanSuggestions(final PsiElement element, final List<LookupElement> answer,
                                       final boolean deprecated, final Object defaultValue) {
        // for boolean types then give a choice between true|false
        LookupElementBuilder builder = createLookupElementBuilder(element, Boolean.TRUE.toString());
        // only show the option in the UI
        builder = builder.withPresentableText(Boolean.TRUE.toString());
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        boolean isDefaultValue = Boolean.TRUE.toString().equals(defaultValue);
        if (isDefaultValue) {
            builder = builder.withTailText(" (default value)");
            // add default value first in the list
            answer.add(0, asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)));
        } else {
            answer.add(asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)));
        }

        builder = createLookupElementBuilder(element, Boolean.FALSE.toString());
        // only show the option in the UI
        builder = builder.withPresentableText(Boolean.FALSE.toString());
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        isDefaultValue = Boolean.FALSE.toString().equals(defaultValue);
        if (isDefaultValue) {
            builder = builder.withTailText(" (default value)");
            // add default value first in the list
            answer.add(0, asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)));
        } else {
            answer.add(asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)));
        }
    }

    /**
     * Adds the possible value suggestions to the given list of {@link LookupElement} in case only a default value
     * is proposed.
     */
    private void addDefaultValueSuggestions(final PsiElement element, final List<LookupElement> answer,
                                            final boolean deprecated, final Object defaultValue) {
        final String lookupString = defaultValue.toString();
        LookupElementBuilder builder = createDefaultValueLookupElementBuilder(element, lookupString)
            .withPresentableText(lookupString);
        // only show the option in the UI
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        builder = builder.withTailText(" (default value)");
        // there is only one value in the list, and it is the default value, so never auto complete it but show as suggestion
        answer.add(0, asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)));
    }

    private static CamelCatalog getCamelCatalog(Project project) {
        return ServiceManager.getService(project, CamelCatalogService.class).get();
    }

    /**
     * To convert the given element into a {@link PrioritizedLookupElement} to ensure that it will be proposed first.
     */
    private static LookupElement asPrioritizedLookupElement(LookupElement element) {
        return PrioritizedLookupElement.withPriority(element, 200.0);
    }

    /**
     * Gives the leaf PSI element corresponding to the position where the completion has been
     * requested.
     * @param parameters the completion parameters from which the current element is retrieved.
     * @return a {@link PsiElement} corresponding to the current position.
     * {@link CompletionParameters#getOriginalPosition()} by default.
     */
    protected @Nullable PsiElement getCompletionPosition(@NotNull CompletionParameters parameters) {
        return parameters.getOriginalPosition();
    }

    /**
     * Gives the prefix to use for the completion.
     *
     * @param element the element from which the new prefix is extracted.
     * @param defaultPrefix the default prefix
     * @return the prefix to use for the completion according to the context.
     */
    protected @NotNull String getPrefix(@NotNull PsiElement element, @NotNull String defaultPrefix) {
        return defaultPrefix;
    }

    /**
     * Creates the {@link LookupElementBuilder} for the given suggestion that may be injected into
     * the given element.
     * @param element the element into which the value of header should be injected.
     * @param suggestion the suggestion to convert into a {@link LookupElementBuilder}.
     * @return a {@link LookupElementBuilder} matching with the given parameters.
     */
    protected abstract LookupElementBuilder createLookupElementBuilder(PsiElement element, String suggestion);

    /**
     * Creates the {@link LookupElementBuilder} for the given suggestion that may be injected into
     * the given element in case of an enum.
     * @param element the element into which the value of header should be injected.
     * @param suggestion the suggestion to convert into a {@link LookupElementBuilder}.
     * @return a {@link LookupElementBuilder} matching with the given parameters.
     */
    protected abstract LookupElementBuilder createEnumLookupElementBuilder(PsiElement element, String suggestion, String javaType);

    /**
     * Creates the {@link LookupElementBuilder} for the given suggestion that may be injected into
     * the given element in case of a default value.
     * @param element the element into which the value of header should be injected.
     * @param suggestion the suggestion to convert into a {@link LookupElementBuilder}.
     * @return a {@link LookupElementBuilder} matching with the given parameters.
     */
    protected abstract LookupElementBuilder createDefaultValueLookupElementBuilder(PsiElement element, String suggestion);

    /**
     * @param headerName the name of header for which we want a predicate.
     * @return a predicate to apply to each header to determine if it should be included
     */
    protected abstract Predicate<ComponentModel.EndpointHeaderModel> predicate(@NotNull String headerName);

    /**
     * Extract the name of header from the given element.
     * @param element the element from which the name of header should be extracted.
     * @return the name of header corresponding to the given element if it can be found, {@code null} otherwise.
     */
    protected abstract @Nullable String getHeaderName(@Nullable PsiElement element);
}
