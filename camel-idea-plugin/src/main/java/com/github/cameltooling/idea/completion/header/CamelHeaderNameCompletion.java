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

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.util.JavaClassUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class of all supported {@link CompletionProvider}s that give the name of potential headers that can be proposed
 * according to the provided source of endpoints.
 */
abstract class CamelHeaderNameCompletion extends CompletionProvider<CompletionParameters> {

    /**
     * The source of endpoints from which we extract the headers to propose.
     */
    private final CamelHeaderEndpointSource source;

    /**
     * Constructs a {@code CamelHeaderEndpointSource} with the given source of endpoints.
     * @param source the source of endpoints from which we extract the name of headers to propose.
     */
    protected CamelHeaderNameCompletion(CamelHeaderEndpointSource source) {
        this.source = source;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet resultSet) {
        final PsiElement element = parameters.getOriginalPosition();
        if (element == null) {
            return;
        }

        final List<LookupElement> answer = new ArrayList<>();
        final CamelCatalog camelCatalog = getCamelCatalog(element.getProject());
        for (CamelHeaderEndpoint endpoint : source.getEndpoints(element)) {
            // it is a known Camel component
            final String json = camelCatalog.componentJSonSchema(endpoint.getComponentName());
            if (json == null) {
                continue;
            }
            final ComponentModel componentModel = JsonMapper.generateComponentModel(json);
            answer.addAll(getSuggestions(componentModel, element, endpoint));
        }
        if (!answer.isEmpty()) {
            // sort the headers A..Z which is easier to users to understand
            answer.sort((o1, o2) -> o1
                .getLookupString()
                .compareToIgnoreCase(o2.getLookupString()));
            final String prefix =  extractTextFromElement(element);
            if (prefix != null) {
                resultSet = resultSet.withPrefixMatcher(prefix);
            }
            resultSet.caseInsensitive().addAllElements(answer);
        }
    }

    private static CamelCatalog getCamelCatalog(Project project) {
        return project.getService(CamelCatalogService.class).get();
    }

    /**
     * @param element the element from which we want to extract the text content.
     * @return the text content that could be extracted from the element.
     */
    protected abstract String extractTextFromElement(PsiElement element);

    /**
     * Gives all the possible suggestions of name of header for the given endpoint.
     * @param component the metadata of the component from which we extract the supported name of headers
     * @param element the element into which the name of header should be injected.
     * @param endpoint the type of endpoint for which we suggest name of headers.
     * @return a list of {@link PrioritizedLookupElement} corresponding to the possible suggestions.
     */
    private List<LookupElement> getSuggestions(final ComponentModel component, final PsiElement element,
                                               final CamelHeaderEndpoint endpoint) {
        final List<LookupElement> answer = new ArrayList<>();
        for (final ComponentModel.EndpointHeaderModel header : component.getEndpointHeaders()) {
            final LookupElement suggestion = getSuggestion(element, endpoint, header);
            if (suggestion != null) {
                answer.add(PrioritizedLookupElement.withPriority(suggestion, 200.0));
            }
        }
        return answer;
    }

    /**
     * @param element the element into which the name of header should be injected.
     * @param endpoint the type of endpoint for which we suggest name of headers.
     * @param header the header for which we expect a name suggestion.
     * @return a {@link LookupElement} representing the suggestion of the name of the given header if it matches with
     * the context, {@code null} otherwise.
     */
    private @Nullable LookupElement getSuggestion(final PsiElement element,
                                                  final CamelHeaderEndpoint endpoint,
                                                  final ComponentModel.EndpointHeaderModel header) {
        if (!"header".equals(header.getKind())) {
            return null;
        }
        // if we are consumer only, then any header that has producer in the label should be skipped (as it is only for producer)
        if (endpoint.isConsumerOnly() && header.getLabel() != null && header.getLabel().contains("producer")) {
            return null;
        }
        // if we are producer only, then any header that has consumer in the label should be skipped (as it is only for consumer)
        if (endpoint.isProducerOnly() && header.getLabel() != null && header.getLabel().contains("consumer")) {
            return null;
        }
        LookupElementBuilder builder = createLookupElementBuilder(element, header);
        // we don't want to highlight the advanced headers which should be more seldom in use
        final boolean advanced = header
            .getGroup()
            .contains("advanced");
        builder = builder.withBoldness(!advanced);
        if (!header.getJavaType().isEmpty()) {
            builder = builder.withTypeText(JavaClassUtils.getService().toSimpleType(header.getJavaType()), true);
        }
        if (header.isDeprecated()) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        // add icons for various headers
        builder = withIcon(element, header, builder);
        return builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE);
    }

    /**
     * Indicates whether a {@code String} literal is expected as suggestion according to the element in which it should
     * be injected.
     * @param element the element into which the name of header should be injected.
     * @return {@code true} if a {@code String} literal is expected as suggestion, {@code false} otherwise.
     */
    protected abstract boolean isStringLiteralExpected(PsiElement element);

    /**
     * Creates the {@link LookupElementBuilder} for the given header corresponding to the suggestion to inject into
     * the given element.
     * @param element the element into which the name of header should be injected.
     * @param header the header for which we expect a name suggestion.
     * @return a {@link LookupElementBuilder} matching with the given parameters.
     */
    protected abstract LookupElementBuilder createLookupElementBuilder(PsiElement element,
                                                                       ComponentModel.EndpointHeaderModel header);

    /**
     * Assigns the icon that matches the best with the given header to the given builder.
     */
    private LookupElementBuilder withIcon(final PsiElement element,
                                          final ComponentModel.EndpointHeaderModel header,
                                          final LookupElementBuilder builder) {
        if (header.isRequired()) {
            return builder.withIcon(AllIcons.Toolwindows.ToolWindowFavorites);
        } else if (header.isSecret()) {
            return builder.withIcon(AllIcons.Nodes.SecurityRole);
        } else if (header.isMultiValue()) {
            return builder.withIcon(AllIcons.General.ArrowRight);
        } else if (header.getEnums() != null) {
            return builder.withIcon(AllIcons.Nodes.Enum);
        } else if ("object".equalsIgnoreCase(header.getType()) || "java.lang.object".equalsIgnoreCase(header.getType())) {
            return builder.withIcon(AllIcons.Nodes.Class);
        } else if (!isStringLiteralExpected(element)) {
            return builder.withIcon(AllIcons.Nodes.Field);
        }
        return builder;
    }
}
