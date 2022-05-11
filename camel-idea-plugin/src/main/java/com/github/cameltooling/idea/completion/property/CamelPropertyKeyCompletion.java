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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.MainModel;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link CompletionProvider} that gives the name of the options of main, components, languages and data formats.
 */
public class CamelPropertyKeyCompletion extends CompletionProvider<CompletionParameters> {

    /**
     * The root prefix of all camel keys.
     */
    private static final String ROOT_KEY_NAME = "camel";
    /**
     * The second part of the prefix of all camel keys corresponding to the configuration of a given component.
     */
    private static final String COMPONENT_KEY_NAME = "component";
    /**
     * The second part of the prefix of all camel keys corresponding to the configuration of a given data format.
     */
    private static final String DATA_FORMAT_KEY_NAME = "dataformat";
    /**
     * The second part of the prefix of all camel keys corresponding to the configuration of a given language.
     */
    private static final String LANGUAGE_KEY_NAME = "language";
    /**
     * The prefix of all camel keys corresponding to the configuration of a given component.
     */
    static final String COMPONENT_KEY_PREFIX = String.format("%s.%s", ROOT_KEY_NAME, COMPONENT_KEY_NAME);
    /**
     * The prefix of all camel keys corresponding to the configuration of a given data format.
     */
    static final String DATA_FORMAT_KEY_PREFIX = String.format("%s.%s", ROOT_KEY_NAME, DATA_FORMAT_KEY_NAME);
    /**
     * The prefix of all camel keys corresponding to the configuration of a language.
     */
    static final String LANGUAGE_KEY_PREFIX = String.format("%s.%s", ROOT_KEY_NAME, LANGUAGE_KEY_NAME);

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        PsiElement element = parameters.getOriginalPosition();
        if (element == null) {
            element = parameters.getPosition();
        }
        final List<LookupElement> answer = getSuggestions(element);
        if (!answer.isEmpty()) {
            // sort the keys A..Z which is easier to users to understand
            answer.sort((o1, o2) -> o1
                .getLookupString()
                .compareToIgnoreCase(o2.getLookupString()));
            result.withPrefixMatcher(getText(element))
                .caseInsensitive()
                .addAllElements(answer);
        }
    }

    /**
     * Extract the text from the given element.
     * @param element the element from which the text is extracted.
     * @return the extracted text content.
     */
    private String getText(final PsiElement element) {
        final String result = element.getText().trim();
        return CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.equals(result) ? "" : result;
    }

    /**
     * Gives all the possible suggestions of key or sub part of key corresponding to the given element.
     * @param element the element from which the context is extracted.
     * @return a list of suggestions corresponding to the given context.
     */
    private List<LookupElement> getSuggestions(final PsiElement element) {
        final String fullKey = getText(element);
        if (isCamelKey(fullKey)) {
            final CamelCatalog camelCatalog = getCamelCatalog(element.getProject());
            final String[] keys = fullKey.split("\\.");
            if (keys.length < 2 || keys.length == 2 && !fullKey.endsWith(".")) {
                return suggestGroups(camelCatalog);
            }
            switch (keys[1]) {
            case COMPONENT_KEY_NAME:
                return suggest(
                    camelCatalog, fullKey, keys, this::suggestComponents, this::suggestComponentOptions
                );
            case DATA_FORMAT_KEY_NAME:
                return suggest(
                    camelCatalog, fullKey, keys, this::suggestDataFormats, this::suggestDataFormatOptions
                );
            case LANGUAGE_KEY_NAME:
                return suggest(
                    camelCatalog, fullKey, keys, this::suggestLanguages, this::suggestLanguageOptions
                );
            default:
                return suggestMainOptions(camelCatalog);
            }
        }
        return List.of();
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @param fullKey the current full content of the key. 
     * @param keys the subsections of the key knowing that the dot character is used as separator.
     * @param suggestNames the function allowing to extract the possible names 
     * @param suggestOptions the function allowing to extract the possible options
     * @return The possible names if the key doesn't have more than 2 sections, the possible options corresponding
     * to the 3 sections of the key otherwise.
     */
    private @NotNull List<LookupElement> suggest(CamelCatalog camelCatalog, String fullKey, String[] keys,
                                                 Function<CamelCatalog, List<LookupElement>> suggestNames,
                                                 BiFunction<CamelCatalog, String, List<LookupElement>> suggestOptions) {
        if (keys.length < 3 || keys.length == 3 && !fullKey.endsWith(".")) {
            return suggestNames.apply(camelCatalog);
        }
        return suggestOptions.apply(camelCatalog, keys[2]);
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @return the suggestion of possible main options that could be extracted from the metadata.
     */
    private @NotNull List<LookupElement> suggestMainOptions(CamelCatalog camelCatalog) {
        final MainModel mainModel = camelCatalog.mainModel();
        if (mainModel == null) {
            return List.of();
        }
        return mainModel.getOptions()
            .stream()
            .map(CamelPropertyKeyCompletion::asOptionNameSuggestion)
            .collect(Collectors.toList());
    }

    /**
     * @param name the name of the component/data format/language for which the options are expected.
     * @param keyPrefix the prefix of the key to use when generating the entire key.
     * @param namesSupplier the supplier of name of components/data formats/languages
     * @param jsonProvider the function allowing to retrieve the json content corresponding to the given name 
     * @param optionsProvider the function allowing to retrieve all the potential options
     * @param optionFilter the filter to apply on the options retrieved from the metadata.
     * @return the list of suggestion of potential options.
     * @param <T> the type of option.
     */
    @NotNull
    private <T extends BaseOptionModel> List<LookupElement> suggestOptions(String name,
                                                                           String keyPrefix,
                                                                           Supplier<List<String>> namesSupplier,
                                                                           UnaryOperator<String> jsonProvider,
                                                                           Function<String, List<T>> optionsProvider,
                                                                           Predicate<T> optionFilter) {
        if (namesSupplier.get().contains(name)) {
            final String json = jsonProvider.apply(name);
            if (json == null) {
                return List.of();
            }
            return optionsProvider.apply(json)
                .stream()
                .filter(optionFilter)
                .map(option ->
                    asOptionNameSuggestion(option, String.format("%s.%s.%s", keyPrefix, name, option.getName()))
                )
                .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @param componentName the name of the component for which the options are extracted.
     * @return the potential options for the given component.
     */
    private @NotNull List<LookupElement> suggestComponentOptions(CamelCatalog camelCatalog, String componentName) {
        return suggestOptions(
            componentName, COMPONENT_KEY_PREFIX, camelCatalog::findComponentNames,
            camelCatalog::componentJSonSchema,
            json -> JsonMapper.generateComponentModel(json).getComponentOptions(),
            option -> "property".equals(option.getKind())
        );
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @param dataFormatName the name of the data format for which the options are extracted.
     * @return the potential options for the given data format.
     */
    private @NotNull List<LookupElement> suggestDataFormatOptions(CamelCatalog camelCatalog, String dataFormatName) {
        return suggestOptions(
            dataFormatName, DATA_FORMAT_KEY_PREFIX, camelCatalog::findDataFormatNames,
            camelCatalog::dataFormatJSonSchema,
            json -> JsonMapper.generateDataFormatModel(json).getOptions(),
            option -> "attribute".equals(option.getKind()) && !"id".equals(option.getName())
        );
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @param languageName the name of the language for which the options are extracted.
     * @return the potential options for the given language.
     */
    private @NotNull List<LookupElement> suggestLanguageOptions(CamelCatalog camelCatalog, String languageName) {
        return suggestOptions(
            languageName, LANGUAGE_KEY_PREFIX, camelCatalog::findLanguageNames,
            camelCatalog::languageJSonSchema,
            json -> JsonMapper.generateLanguageModel(json).getOptions(),
            option -> "attribute".equals(option.getKind()) && !"id".equals(option.getName())
        );
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @return the list potential name of components.
     */
    @NotNull
    private List<LookupElement> suggestComponents(CamelCatalog camelCatalog) {
        return suggestNames(camelCatalog::findComponentNames, COMPONENT_KEY_PREFIX);
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @return the list potential name of data format.
     */
    @NotNull
    private List<LookupElement> suggestDataFormats(CamelCatalog camelCatalog) {
        return suggestNames(camelCatalog::findDataFormatNames, DATA_FORMAT_KEY_PREFIX);
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @return the list potential name of language.
     */
    @NotNull
    private List<LookupElement> suggestLanguages(CamelCatalog camelCatalog) {
        return suggestNames(camelCatalog::findLanguageNames, LANGUAGE_KEY_PREFIX);
    }

    /**
     * @param namesSupplier the supplier of name of component/language/data format.
     * @param keyPrefix the prefix of the key to generate
     * @return the list of potential part of keys.
     */
    @NotNull
    private List<LookupElement> suggestNames(Supplier<List<String>> namesSupplier, String keyPrefix) {
        return namesSupplier.get()
            .stream()
            .map(key -> String.format("%s.%s", keyPrefix, key))
            .map(CamelPropertyKeyCompletion::asPrefixSuggestion)
            .collect(Collectors.toList());
    }

    /**
     * @param camelCatalog the catalog from which the metadata are extracted.
     * @return all the suggestion of groups
     */
    private @NotNull List<LookupElement> suggestGroups(@NotNull CamelCatalog camelCatalog) {
        final MainModel mainModel = camelCatalog.mainModel();
        if (mainModel == null) {
            return List.of();
        }
        final List<MainModel.MainGroupModel> options = mainModel.getGroups();
        final List<LookupElement> result = new ArrayList<>(options.size() + 1);
        result.add(asPrefixSuggestion(COMPONENT_KEY_PREFIX));
        result.add(asPrefixSuggestion(DATA_FORMAT_KEY_PREFIX));
        result.add(asPrefixSuggestion(LANGUAGE_KEY_PREFIX));
        options
            .stream()
            .map(MainModel.MainGroupModel::getName)
            .map(CamelPropertyKeyCompletion::asPrefixSuggestion)
            .forEach(result::add);
        return result;
    }

    /**
     * Indicates whether the given key is a camel key.
     * @param key the key to check.
     * @return {@code true} if the key is empty or starts with {@code camel}, {@code false} otherwise.
     */
    private static boolean isCamelKey(String key) {
        return key.isEmpty()
            || key.length() < ROOT_KEY_NAME.length() && key.startsWith(ROOT_KEY_NAME.substring(0, key.length()))
            || key.startsWith(ROOT_KEY_NAME);
    }

    /**
     * To convert the given element into a {@link PrioritizedLookupElement} to ensure that it will be proposed first.
     */
    private static LookupElement asPrioritizedLookupElement(LookupElement element) {
        return PrioritizedLookupElement.withPriority(element, 200.0);
    }

    /**
     * @param suggestion the suggestion to convert as a {@code LookupElement}.
     * @return a {@code LookupElement} corresponding to a suggestion of the beginning of the entire key.
     */
    private static LookupElement asPrefixSuggestion(String suggestion) {
        return asPrioritizedLookupElement(
            LookupElementBuilder.create(String.format("%s.", suggestion))
                .withLookupString(suggestion)
                .withPresentableText(suggestion)
        );
    }

    /**
     * @param option the option to convert as a {@code LookupElement}.
     * @return a {@code LookupElement} corresponding to the entire key representing the given option.
     */
    private static LookupElement asOptionNameSuggestion(BaseOptionModel option) {
        return asOptionNameSuggestion(option, option.getName());
    }


    /**
     * @param option the option to convert as a {@code LookupElement}.
     * @param suggestion the original suggestion to convert
     * @return a {@code LookupElement} corresponding to the entire key representing the given option.
     */
    private static LookupElement asOptionNameSuggestion(BaseOptionModel option, String suggestion) {
        final String suggestionInKebabCase = toKebabCaseLeaf(suggestion);
        LookupElementBuilder builder = LookupElementBuilder.create(String.format("%s = ", suggestionInKebabCase))
            .withLookupString(suggestionInKebabCase)
            .withLookupString(suggestion)
            .withPresentableText(suggestionInKebabCase);
        // we don't want to highlight the advanced headers which should be more seldom in use
        final String group = option.getGroup();
        final boolean advanced = group != null && group.contains("advanced");
        builder = builder.withBoldness(!advanced);
        if (!option.getJavaType().isEmpty()) {
            builder = builder.withTypeText(option.getJavaType(), true);
        }
        if (option.isDeprecated()) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        // add icons for various headers
        builder = withIcon(option, builder);
        return asPrioritizedLookupElement(
            builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
        );
    }

    private static CamelCatalog getCamelCatalog(Project project) {
        return ServiceManager.getService(project, CamelCatalogService.class).get();
    }

    /**
     * Assigns the icon that matches the best with the given option to the given builder.
     */
    private static LookupElementBuilder withIcon(final BaseOptionModel option, final LookupElementBuilder builder) {
        if (option.isRequired()) {
            return builder.withIcon(AllIcons.Toolwindows.ToolWindowFavorites);
        } else if (option.isSecret()) {
            return builder.withIcon(AllIcons.Nodes.SecurityRole);
        } else if (option.isMultiValue()) {
            return builder.withIcon(AllIcons.General.ArrowRight);
        } else if (option.getEnums() != null) {
            return builder.withIcon(AllIcons.Nodes.Enum);
        } else if ("object".equalsIgnoreCase(option.getType()) || "java.lang.object".equalsIgnoreCase(option.getType())) {
            return builder.withIcon(AllIcons.Nodes.Class);
        }
        return builder;
    }

    /**
     * Convert the content after the last dot in kebab case.
     * @param key the key to convert.
     * @return the content of the given key with the last section of the key in kebab case.
     */
    private static String toKebabCaseLeaf(String key) {
        final int lastIndex = key.lastIndexOf('.');
        final int length = key.length();
        final StringBuilder result = new StringBuilder(64);
        for (int i = 0; i < length; i++) {
            char c = key.charAt(i);
            if (i > lastIndex && Character.isUpperCase(c)) {
                result.append('-');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
