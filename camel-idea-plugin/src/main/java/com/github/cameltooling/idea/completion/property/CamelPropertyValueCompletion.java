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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.parsing.PropertiesElementTypes;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.MainModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.completion.property.CamelPropertyKeyCompletion.COMPONENT_KEY_PREFIX;
import static com.github.cameltooling.idea.completion.property.CamelPropertyKeyCompletion.DATA_FORMAT_KEY_PREFIX;
import static com.github.cameltooling.idea.completion.property.CamelPropertyKeyCompletion.LANGUAGE_KEY_PREFIX;
import static com.github.cameltooling.idea.util.StringUtils.fromKebabToCamelCase;

/**
 * The {@link CompletionProvider} that gives the value of the options of main, components, languages and data formats.
 */
public class CamelPropertyValueCompletion extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        PsiElement element = parameters.getOriginalPosition();
        if (element == null) {
            element = parameters.getPosition();
        }
        if (element.getProject().getService(CamelService.class).isCamelPresent()) {
            final String propertyKey = getPropertyKey(element);
            if (propertyKey == null) {
                return;
            }
            final Optional<? extends BaseOptionModel> option = getOption(element, propertyKey);
            if (option.isEmpty()) {
                return;
            }
            final List<LookupElement> answer = getSuggestions(option.get());
            if (!answer.isEmpty()) {
                // sort the values A..Z which is easier to users to understand
                answer.sort((o1, o2) -> o1
                    .getLookupString()
                    .compareToIgnoreCase(o2.getLookupString()));
                result
                    .caseInsensitive()
                    .addAllElements(answer);
            }
        }
    }

    /**
     * Gives all the possible suggestions of values for the given option.
     *
     * @param option the option for which we expect value suggestions.
     * @return a list of {@link LookupElement} corresponding to the possible suggestions.
     */
    private static List<LookupElement> getSuggestions(final BaseOptionModel option) {
        final List<LookupElement> answer = new ArrayList<>();

        final String javaType = option.getJavaType();
        final boolean deprecated = option.isDeprecated();
        final List<String> enums = option.getEnums();
        final Object defaultValue = option.getDefaultValue();

        if (enums != null) {
            addEnumSuggestions(option, answer, deprecated, enums, defaultValue);
        } else if ("java.lang.Boolean".equalsIgnoreCase(javaType) || "boolean".equalsIgnoreCase(javaType)) {
            addBooleanSuggestions(option, answer, deprecated, defaultValue);
        } else if (defaultValue != null) {
            // for any other kind of type and if there is a default value then add that as a suggestion
            // so it is easy to see what the default value is
            addDefaultValueSuggestions(option, answer, deprecated, defaultValue);
        }

        return answer;
    }

    /**
     * Adds the possible value suggestions to the given list of {@link LookupElement} in case the value is an
     * enum.
     */
    private static void addEnumSuggestions(final BaseOptionModel option, final List<LookupElement> answer,
                                           final boolean deprecated, final List<String> enums,
                                           final Object defaultValue) {
        for (String part : enums) {
            LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, part));
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
    private static void addBooleanSuggestions(final BaseOptionModel option, final List<LookupElement> answer,
                                              final boolean deprecated, final Object defaultValue) {
        // for boolean types then give a choice between true|false
        LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, Boolean.TRUE.toString()));
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

        builder = LookupElementBuilder.create(new OptionSuggestion(option, Boolean.FALSE.toString()));
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
    private static void addDefaultValueSuggestions(final BaseOptionModel option, final List<LookupElement> answer,
                                                   final boolean deprecated, final Object defaultValue) {
        final String lookupString = defaultValue.toString();
        LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, lookupString));
        // only show the option in the UI
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        builder = builder.withTailText(" (default value)");
        // there is only one value in the list, and it is the default value, so never auto complete it but show as suggestion
        answer.add(0, asPrioritizedLookupElement(builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)));
    }

    /**
     * To convert the given element into a {@link PrioritizedLookupElement} to ensure that it will be proposed first.
     */
    private static LookupElement asPrioritizedLookupElement(LookupElement element) {
        return PrioritizedLookupElement.withPriority(element, 200.0);
    }

    /**
     * Finds the option among the options of main, component, data format and language that match with the given
     * property key.
     * @param element the element from which the catalog is extracted.
     * @param propertyKey the key of the property for which we are looking for the corresponding option.
     * @return the corresponding option if it could be found, {@code Optional.empty()} otherwise.
     */
    private static @NotNull Optional<? extends BaseOptionModel> getOption(@NotNull PsiElement element,
                                                                          @NotNull String propertyKey) {
        final CamelCatalog camelCatalog = getCamelCatalog(element.getProject());
        if (propertyKey.startsWith(COMPONENT_KEY_PREFIX)) {
            return getOption(
                propertyKey, camelCatalog::findComponentNames,
                camelCatalog::componentJSonSchema,
                json -> JsonMapper.generateComponentModel(json).getComponentOptions()
            );
        } else if (propertyKey.startsWith(DATA_FORMAT_KEY_PREFIX)) {
            return getOption(
                propertyKey, camelCatalog::findDataFormatNames,
                camelCatalog::dataFormatJSonSchema,
                json -> JsonMapper.generateDataFormatModel(json).getOptions()
            );

        } else if (propertyKey.startsWith(LANGUAGE_KEY_PREFIX)) {
            return getOption(
                propertyKey, camelCatalog::findLanguageNames,
                camelCatalog::languageJSonSchema,
                json -> JsonMapper.generateLanguageModel(json).getOptions()
            );

        }
        // It is a main property
        final MainModel mainModel = camelCatalog.mainModel();
        if (mainModel == null) {
            return Optional.empty();
        }
        return mainModel.getOptions()
            .stream()
            .filter(option -> isEquals(propertyKey, option.getName()))
            .findFirst();
    }

    /**
     * @param propertyKey the property key for which we want the corresponding option.
     * @param namesSupplier the supplier of name of components/data formats/languages
     * @param jsonProvider the function allowing to retrieve the json content corresponding to the given name
     * @param optionsProvider the function allowing to retrieve all the potential options
     * @return the option that matches with the given property key if any, {@code Optional.empty()} otherwise.
     * @param <T> the type of option
     */
    @NotNull
    private static <T extends BaseOptionModel> Optional<T> getOption(@NotNull String propertyKey,
                                                                     Supplier<List<String>> namesSupplier,
                                                                     UnaryOperator<String> jsonProvider,
                                                                     Function<String, List<T>> optionsProvider) {
        final String[] keys = propertyKey.split("\\.");
        if (keys.length != 4) {
            return Optional.empty();
        }
        final String name = keys[2];
        if (namesSupplier.get().contains(name)) {
            final String json = jsonProvider.apply(name);
            if (json == null) {
                return Optional.empty();
            }
            return optionsProvider.apply(json)
                .stream()
                .filter(option -> isEquals(keys[3], option.getName()))
                .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Indicates whether the given property key is equal to the given option whatever the case used (Camel or Kebab)
     * @param propertyKey the property key to compare with.
     * @param optionName the name of the option to compare with
     * @return {@code true} if they are equal, {@code false} otherwise.
     */
    private static boolean isEquals(@NotNull String propertyKey, String optionName) {
        // The option name can be in Camel Case or Kebab Case
        return propertyKey.equals(optionName) || fromKebabToCamelCase(propertyKey).equals(fromKebabToCamelCase(optionName));
    }

    private static CamelCatalog getCamelCatalog(Project project) {
        return project.getService(CamelCatalogService.class).get();
    }

    /**
     * Gets the key of the property corresponding to the given element.
     * @param element the element from which the property key must be retrieved.
     * @return the corresponding property key if it could be found {@code null} otherwise.
     */
    private static @Nullable String getPropertyKey(PsiElement element) {
        PsiElement previous = element.getPrevSibling();
        while (previous != null) {
            final ASTNode node = previous.getNode();
            if (node != null) {
                final IElementType elementType = node.getElementType();
                if (elementType == PropertiesElementTypes.PROPERTY) {
                    final String result = node.getText();
                    return result.endsWith("=") ? result.substring(0, result.length() - 1) : result;
                } else if (elementType == PropertiesTokenTypes.KEY_CHARACTERS) {
                    return node.getText();
                }
            }
            previous = previous.getPrevSibling();
        }
        return null;
    }
}
