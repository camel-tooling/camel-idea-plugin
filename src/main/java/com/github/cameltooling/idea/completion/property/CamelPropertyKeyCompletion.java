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
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.JBangModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.MainModel;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link CompletionProvider} that gives the name of the options of main, components, languages and data formats.
 */
abstract class CamelPropertyKeyCompletion extends CompletionProvider<CompletionParameters> {

    /**
     * The prefix of all camel keys corresponding to the configuration of a given component.
     */
    static final String COMPONENT_KEY_PREFIX = "camel.component";
    /**
     * The prefix of all camel keys corresponding to the configuration of a given data format.
     */
    static final String DATA_FORMAT_KEY_PREFIX = "camel.dataformat";
    /**
     * The prefix of all camel keys corresponding to the configuration of a language.
     */
    static final String LANGUAGE_KEY_PREFIX = "camel.language";
    /**
     * The prefix of all camel keys corresponding to the configuration of a main.
     */
    static final String MAIN_KEY_PREFIX = "camel.main";
    /**
     * The prefix of all camel keys corresponding to the configuration of a jbang.
     */
    static final String JBANG_KEY_PREFIX = "camel.jbang";
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
     * The second part of the prefix of all camel keys corresponding to the configuration of main options.
     */
    private static final String MAIN_KEY_NAME = "main";
    /**
     * The second part of the prefix of all camel keys corresponding to the configuration of jbang options.
     */
    private static final String JBANG_KEY_NAME = "jbang";

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        final PsiElement element = getCompletionPosition(parameters);
        if (isEnabled(element.getProject(), element.getContainingFile())) {
            final List<LookupElement> answer = getSuggestions(element);
            if (!answer.isEmpty()) {
                // sort the keys A..Z which is easier to users to understand and get rid od duplicates
                final Map<String, LookupElement> map = new TreeMap<>();
                answer.forEach(
                    lookupElement -> map.putIfAbsent(lookupElement.getLookupString(), lookupElement)
                );
                // Remove empty proposal
                map.remove("");
                result.withPrefixMatcher(getPrefix(element))
                    .caseInsensitive()
                    .addAllElements(map.values());
            }
        }
    }

    /**
     * Indicates whether suggestions should be provided for the given file in the given project
     *
     * @param project the project to test.
     * @param file the file to test
     * @return {@code true} if the code completion is enabled for the given file, {@code false} otherwise.
     */
    protected abstract boolean isEnabled(Project project, PsiFile file);

    /**
     * Gives the leaf PSI element corresponding to the position where the completion has been
     * requested.
     * @param parameters the completion parameters from which the current element is retrieved.
     * @return a {@link PsiElement} corresponding to the current position.
     */
    protected abstract @NotNull PsiElement getCompletionPosition(@NotNull CompletionParameters parameters);

    /**
     * Extract the full property key from the given element.
     *
     * @param element the element from which the full property key is extracted.
     * @return the extracted full property key.
     */
    protected abstract String getFullKey(@NotNull PsiElement element);

    /**
     * Extract the prefix allowing to filter out the suggestion from the given element.
     * @param element  the element from which the prefix is extracted.
     * @return the extracted prefix.
     */
    protected abstract String getPrefix(@NotNull PsiElement element);

    /**
     * @param context             the context of the suggestion.
     * @param suggestion          the prefix suggestion for which the corresponding {@code LookupElementBuilder} is expected.
     * @param descriptionSupplier the supplier of the description of the suggestion
     * @return a {@code LookupElementBuilder} corresponding to a suggestion of the beginning of the entire key.
     */
    protected abstract LookupElementBuilder createLookupElementBuilderForPrefixSuggestion(SuggestionContext context,
                                                                                          String suggestion,
                                                                                          Supplier<String> descriptionSupplier);

    /**
     * @param context    the context of the suggestion.
     * @param suggestion the prefix suggestion for which the corresponding {@code LookupElementBuilder} is expected.
     * @return a {@code LookupElementBuilder} corresponding to a suggestion of the beginning of the entire key.
     */
    protected abstract LookupElementBuilder createLookupElementBuilderForPrefixSuggestion(SuggestionContext context,
                                                                                          String suggestion);

    /**
     * @param context               the context of the suggestion.
     * @param option                the option for which the corresponding {@code LookupElementBuilder} is expected.
     * @param suggestion            the original suggestion to convert to {@code LookupElementBuilder}
     * @param suggestionInKebabCase the original suggestion in kebab case to convert to {@code LookupElementBuilder}
     * @return a {@code LookupElementBuilder} corresponding to the entire key representing the given option.
     */
    protected abstract LookupElementBuilder createLookupElementBuilderForOptionNameSuggestion(SuggestionContext context,
                                                                                              BaseOptionModel option,
                                                                                              String suggestion,
                                                                                              String suggestionInKebabCase);

    /**
     * Gives all the possible suggestions of keys or sub part of keys corresponding to the given element.
     *
     * @param element the element from which the context is extracted.
     * @return a list of suggestions corresponding to the given context.
     */
    private List<LookupElement> getSuggestions(final PsiElement element) {
        final String fullKey = getFullKey(element);
        final SuggestionContext context = new SuggestionContext(getCamelCatalog(element.getProject()), fullKey);
        final String[] keys = context.getKeys();
        if (keys.length < 2 || keys.length == 2 && !fullKey.endsWith(".")) {
            return suggestGroups(context);
        }
        switch (keys[1]) {
        case COMPONENT_KEY_NAME:
            return suggest(context, this::suggestComponents, this::suggestComponentOptions);
        case DATA_FORMAT_KEY_NAME:
            return suggest(context, this::suggestDataFormats, this::suggestDataFormatOptions);
        case LANGUAGE_KEY_NAME:
            return suggest(context, this::suggestLanguages, this::suggestLanguageOptions);
        case JBANG_KEY_NAME:
            return suggestJBangOptions(context);
        default:
            return suggestMainOptions(context);
        }
    }

    /**
     * @param context        the context of the suggestion.
     * @param suggestNames   the function allowing to extract the possible names
     * @param suggestOptions the function allowing to extract the possible options
     * @return The possible names if the key doesn't have more than 2 sections, the possible options corresponding
     * to the 3 sections of the key otherwise.
     */
    private @NotNull List<LookupElement> suggest(final SuggestionContext context,
                                                 final Function<SuggestionContext, List<LookupElement>> suggestNames,
                                                 final BiFunction<SuggestionContext, String, List<LookupElement>> suggestOptions) {
        final String[] keys = context.getKeys();
        if (keys.length < 3 || keys.length == 3 && !context.getFullKey().endsWith(".")) {
            return suggestNames.apply(context);
        }
        return suggestOptions.apply(context, keys[2]);
    }

    /**
     * @param context the context of the suggestion.
     * @return the suggestion of possible main options that could be extracted from the metadata.
     */
    private @NotNull List<LookupElement> suggestMainOptions(final SuggestionContext context) {
        final MainModel mainModel = context.getCamelCatalog().mainModel();
        if (mainModel == null) {
            return List.of();
        }
        return mainModel.getOptions()
            .stream()
            .map(option -> asOptionNameSuggestion(context, option))
            .collect(Collectors.toList());
    }

    /**
     * @param context the context of the suggestion.
     * @return the suggestion of possible jbang options that could be extracted from the metadata.
     */
    private @NotNull List<LookupElement> suggestJBangOptions(final SuggestionContext context) {
        final JBangModel jbangModel = context.getCamelCatalog().jbangModel();
        if (jbangModel == null) {
            return List.of();
        }
        return jbangModel.getOptions()
                .stream()
                .map(option -> asOptionNameSuggestion(context, option))
                .collect(Collectors.toList());
    }

    /**
     * @param context         the context of the suggestion.
     * @param name            the name of the component/data format/language for which the options are expected.
     * @param keyPrefix       the prefix of the key to use when generating the entire key.
     * @param namesSupplier   the supplier of name of components/data formats/languages
     * @param jsonProvider    the function allowing to retrieve the json content corresponding to the given name
     * @param modelProvider   the function allowing to parse the model from a json payload
     * @param optionsProvider the function allowing to retrieve all the potential options from the model
     * @param optionFilter    the filter to apply on the options retrieved from the metadata.
     * @param <T>             the type of model.
     * @param <O>             the type of option.
     * @return the list of suggestion of potential options.
     */
    @NotNull
    private <T extends ArtifactModel<O>, O extends BaseOptionModel> List<LookupElement> suggestOptions(final SuggestionContext context,
                                                                                                       final String name,
                                                                                                       final String keyPrefix,
                                                                                                       final Supplier<List<String>> namesSupplier,
                                                                                                       final UnaryOperator<String> jsonProvider,
                                                                                                       final Function<String, T> modelProvider,
                                                                                                       final Function<T, List<O>> optionsProvider,
                                                                                                       final Predicate<O> optionFilter) {
        if (namesSupplier.get().contains(name)) {
            final String json = jsonProvider.apply(name);
            if (json == null) {
                return List.of();
            }
            final T model = modelProvider.apply(json);
            if (model == null) {
                return List.of();
            }
            return optionsProvider.apply(model)
                .stream()
                .filter(optionFilter)
                .map(option ->
                    asOptionNameSuggestion(context, option, String.format("%s.%s.%s", keyPrefix, name, option.getName()))
                )
                .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * @param context       the context of the suggestion.
     * @param componentName the name of the component for which the options are extracted.
     * @return the potential options for the given component.
     */
    private @NotNull List<LookupElement> suggestComponentOptions(final SuggestionContext context,
                                                                 final String componentName) {
        final CamelCatalog camelCatalog = context.getCamelCatalog();
        return suggestOptions(
            context, componentName, COMPONENT_KEY_PREFIX, camelCatalog::findComponentNames,
            camelCatalog::componentJSonSchema, JsonMapper::generateComponentModel,
            ComponentModel::getComponentOptions, option -> "property".equals(option.getKind())
        );
    }

    /**
     * @param context        the context of the suggestion.
     * @param dataFormatName the name of the data format for which the options are extracted.
     * @return the potential options for the given data format.
     */
    private @NotNull List<LookupElement> suggestDataFormatOptions(final SuggestionContext context,
                                                                  final String dataFormatName) {
        final CamelCatalog camelCatalog = context.getCamelCatalog();
        return suggestOptions(
            context, dataFormatName, DATA_FORMAT_KEY_PREFIX, camelCatalog::findDataFormatNames,
            camelCatalog::dataFormatJSonSchema, JsonMapper::generateDataFormatModel,
            DataFormatModel::getOptions, option -> "attribute".equals(option.getKind()) && !"id".equals(option.getName())
        );
    }

    /**
     * @param context      the context of the suggestion.
     * @param languageName the name of the language for which the options are extracted.
     * @return the potential options for the given language.
     */
    private @NotNull List<LookupElement> suggestLanguageOptions(final SuggestionContext context,
                                                                final String languageName) {
        final CamelCatalog camelCatalog = context.getCamelCatalog();
        return suggestOptions(
            context, languageName, LANGUAGE_KEY_PREFIX, camelCatalog::findLanguageNames,
            camelCatalog::languageJSonSchema, JsonMapper::generateLanguageModel,
            LanguageModel::getOptions, option -> "attribute".equals(option.getKind()) && !"id".equals(option.getName())
        );
    }

    /**
     * @param context the context of the suggestion.
     * @return the list potential name of components.
     */
    @NotNull
    private List<LookupElement> suggestComponents(final SuggestionContext context) {
        final CamelCatalog camelCatalog = context.getCamelCatalog();
        return suggestNames(
            context, camelCatalog::findComponentNames, COMPONENT_KEY_PREFIX, camelCatalog::componentJSonSchema,
            JsonMapper::generateComponentModel
        );
    }

    /**
     * @param context the context of the suggestion.
     * @return the list potential name of data format.
     */
    @NotNull
    private List<LookupElement> suggestDataFormats(final SuggestionContext context) {
        final CamelCatalog camelCatalog = context.getCamelCatalog();
        return suggestNames(
            context, camelCatalog::findDataFormatNames, DATA_FORMAT_KEY_PREFIX, camelCatalog::dataFormatJSonSchema,
            JsonMapper::generateDataFormatModel
        );
    }

    /**
     * @param context the context of the suggestion.
     * @return the list potential name of language.
     */
    @NotNull
    private List<LookupElement> suggestLanguages(final SuggestionContext context) {
        final CamelCatalog camelCatalog = context.getCamelCatalog();
        return suggestNames(
            context, camelCatalog::findLanguageNames, LANGUAGE_KEY_PREFIX, camelCatalog::languageJSonSchema,
            JsonMapper::generateLanguageModel
        );
    }

    /**
     * @param context       the context of the suggestion.
     * @param namesSupplier the supplier of name of component/language/data format.
     * @param keyPrefix     the prefix of the key to generate
     * @param jsonProvider  the function allowing to retrieve the json content corresponding to the given name
     * @param modelProvider the function allowing to parse the model from a json payload
     * @return the list of potential part of keys.
     */
    @NotNull
    private <T extends ArtifactModel<O>, O extends BaseOptionModel> List<LookupElement> suggestNames(final SuggestionContext context,
                                                                                                     final Supplier<List<String>> namesSupplier,
                                                                                                     final String keyPrefix,
                                                                                                     final UnaryOperator<String> jsonProvider,
                                                                                                     final Function<String, T> modelProvider) {
        final Function<String, Supplier<String>> descriptionSupplierProvider =
            key ->
                () -> {
                    final String json = jsonProvider.apply(key);
                    if (json == null) {
                        return null;
                    }
                    final T model = modelProvider.apply(json);
                    if (model == null) {
                        return null;
                    }
                    return model.getDescription();
                };
        return namesSupplier.get()
            .stream()
            .map(key -> asPrefixSuggestion(context, String.format("%s.%s", keyPrefix, key), descriptionSupplierProvider.apply(key)))
            .collect(Collectors.toList());
    }

    /**
     * @param context the context of the suggestion.
     * @return all the suggestion of groups
     */
    private @NotNull List<LookupElement> suggestGroups(final SuggestionContext context) {
        final MainModel mainModel = context.getCamelCatalog().mainModel();
        if (mainModel == null) {
            return List.of();
        }
        final List<MainModel.MainGroupModel> options = mainModel.getGroups();
        final List<LookupElement> result = new ArrayList<>(options.size() + 1);
        result.add(asPrefixSuggestion(context, COMPONENT_KEY_PREFIX));
        result.add(asPrefixSuggestion(context, DATA_FORMAT_KEY_PREFIX));
        result.add(asPrefixSuggestion(context, LANGUAGE_KEY_PREFIX));
        options
            .stream()
            .map(option -> asPrefixSuggestion(context, option))
            .forEach(result::add);
        result.add(asPrefixSuggestion(context, JBANG_KEY_PREFIX, () -> "Camel JBang"));
        return result;
    }

    /**
     * To convert the given element into a {@link PrioritizedLookupElement} to ensure that it will be proposed first.
     */
    private static LookupElement asPrioritizedLookupElement(LookupElement element) {
        return PrioritizedLookupElement.withPriority(element, 200.0);
    }

    /**
     * @param context             the context of the suggestion.
     * @param suggestion          the suggestion to convert as a {@code LookupElement}.
     * @param descriptionSupplier the supplier of the description of the suggestion
     * @return a {@code LookupElement} corresponding to a suggestion of the beginning of the entire key.
     */
    private LookupElement asPrefixSuggestion(final SuggestionContext context, final String suggestion,
                                             final Supplier<String> descriptionSupplier) {
        return asPrioritizedLookupElement(createLookupElementBuilderForPrefixSuggestion(context, suggestion, descriptionSupplier));
    }

    /**
     * @param context the context of the suggestion.
     * @param main    the main group's suggestion to convert as a {@code LookupElement}.
     * @return a {@code LookupElement} corresponding to a suggestion of the beginning of the entire key.
     */
    private LookupElement asPrefixSuggestion(final SuggestionContext context, final MainModel.MainGroupModel main) {
        return asPrefixSuggestion(context, main.getName(), main::getDescription);
    }

    /**
     * @param context    the context of the suggestion.
     * @param suggestion the suggestion to convert as a {@code LookupElement}.
     * @return a {@code LookupElement} corresponding to a suggestion of the beginning of the entire key.
     */
    private LookupElement asPrefixSuggestion(final SuggestionContext context, final String suggestion) {
        return asPrioritizedLookupElement(createLookupElementBuilderForPrefixSuggestion(context, suggestion));
    }

    /**
     * @param context the context of the suggestion.
     * @param option  the option to convert as a {@code LookupElement}.
     * @return a {@code LookupElement} corresponding to the entire key representing the given option.
     */
    private LookupElement asOptionNameSuggestion(final SuggestionContext context, final BaseOptionModel option) {
        return asOptionNameSuggestion(context, option, option.getName());
    }

    /**
     * @param context    the context of the suggestion.
     * @param option     the option to convert as a {@code LookupElement}.
     * @param suggestion the original suggestion to convert
     * @return a {@code LookupElement} corresponding to the entire key representing the given option.
     */
    private LookupElement asOptionNameSuggestion(final SuggestionContext context, final BaseOptionModel option,
                                                 final String suggestion) {
        LookupElementBuilder builder = createLookupElementBuilderForOptionNameSuggestion(
            context, option, suggestion, toKebabCaseLeaf(suggestion)
        );
        if (builder.getObject() instanceof String) {
            // Only the sub part has been taken into account so no type should be added
            return asPrioritizedLookupElement(builder);
        }
        // we don't want to highlight the advanced headers which should be more seldom in use
        final String group = option.getGroup();
        final boolean advanced = group != null && group.contains("advanced");
        builder = builder.withBoldness(!advanced);
        if (option.getJavaType() != null && !option.getJavaType().isEmpty()) {
            builder = builder.withTypeText(JavaClassUtils.getService().toSimpleType(option.getJavaType()), true);
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
        return project.getService(CamelCatalogService.class).get();
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
     *
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

    /**
     * {@code SuggestionContext} holds all the object instances needed for a suggestion.
     */
    static class SuggestionContext {

        /**
         * The catalog from which the metadata are extracted.
         */
        private final CamelCatalog camelCatalog;
        /**
         * The full content of the current key.
         */
        private final String fullKey;
        /**
         * The subsections of the key knowing that the dot character is used as separator.
         */
        private final String[] keys;

        /**
         * Construct a {@code SuggestionContext} with the given parameters.
         *
         * @param camelCatalog the catalog from which the metadata are extracted.
         * @param fullKey      the full content of the current key.
         */
        SuggestionContext(@NotNull CamelCatalog camelCatalog, @NotNull String fullKey) {
            this.camelCatalog = camelCatalog;
            this.fullKey = fullKey;
            this.keys = fullKey.split("\\.");
        }

        CamelCatalog getCamelCatalog() {
            return camelCatalog;
        }

        String getFullKey() {
            return fullKey;
        }

        String[] getKeys() {
            return keys;
        }
    }
}
