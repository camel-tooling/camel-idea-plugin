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
package com.github.cameltooling.idea.completion.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.*;

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.JavaClassUtils;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;

/**
 * Smart completion for editing a Camel endpoint uri, to show a list of possible endpoint options which can be added.
 * For example editing <tt>jms:queue?_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible options for the JMS endpoint.
 */
public final class CamelSmartCompletionEndpointOptions {

    private CamelSmartCompletionEndpointOptions() {
        // static class
    }

    @NotNull
    public static List<LookupElement> addSmartCompletionSuggestionsQueryParameters(final String[] query,
                                                                                   final ComponentModel component,
                                                                                   final Map<String, String> existing,
                                                                                   final boolean xmlMode,
                                                                                   final PsiElement element,
                                                                                   final Editor editor) {
        final List<LookupElement> answer = new ArrayList<>();

        String queryAtPosition = query[2];
        if (xmlMode) {
            queryAtPosition = queryAtPosition.replace("&amp;", "&");
        }

        final List<ComponentModel.EndpointOptionModel> options = component.getEndpointOptions();
        // sort the options A..Z which is easier to users to understand
        options.sort((o1, o2) -> o1
                .getName()
                .compareToIgnoreCase(o2.getName()));
        queryAtPosition = removeUnknownOption(queryAtPosition, existing, element);

        for (final ComponentModel.EndpointOptionModel option : options) {

            if ("parameter".equals(option.getKind())) {
                final String name = option.getName();

                final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();

                // if we are consumer only, then any option that has producer in the label should be skipped (as its only for producer)
                final boolean consumerOnly = camelIdeaUtils.isConsumerEndpoint(element);
                if (consumerOnly && option.getLabel() != null && option.getLabel().contains("producer")) {
                    continue;
                }
                // if we are producer only, then any option that has consumer in the label should be skipped (as its only for consumer)
                final boolean producerOnly = camelIdeaUtils.isProducerEndpoint(element);
                if (producerOnly && option.getLabel() != null && option.getLabel().contains("consumer")) {
                    continue;
                }

                // only add if not already used (or if the option is multi valued then it can have many)
                final String old = existing != null ? existing.get(name) : "";
                if (option.isMultiValue() || existing == null || old == null || old.isEmpty()) {

                    // no tail for prefix, otherwise use = to setup for value
                    final String key = option.getPrefix() != null ? option.getPrefix() : name;

                    // the lookup should prepare for the new option
                    String lookup;
                    final String concatQuery = query[0];
                    if (!concatQuery.contains("?")) {
                        // none existing options so we need to start with a ? mark
                        lookup = queryAtPosition + "?" + key;
                    } else {
                        if (!queryAtPosition.endsWith("&") && !queryAtPosition.endsWith("?")) {
                            lookup = queryAtPosition + "&" + key;
                        } else {
                            // there is already either an ending ? or &
                            lookup = queryAtPosition + key;
                        }
                    }
                    if (xmlMode) {
                        lookup = lookup.replace("&", "&amp;");
                    }
                    LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, lookup));
                    final String suffix = query[1];
                    builder = addInsertHandler(editor, builder, suffix);
                    // only show the option in the UI
                    builder = builder.withPresentableText(name);
                    // we don't want to highlight the advanced options which should be more seldom in use
                    final boolean advanced = option
                            .getGroup()
                            .contains("advanced");
                    builder = builder.withBoldness(!advanced);
                    if (!option.getJavaType().isEmpty()) {
                        builder = builder.withTypeText(JavaClassUtils.getService().toSimpleType(option.getJavaType()), true);
                    }
                    if (option.isDeprecated()) {
                        // mark as deprecated
                        builder = builder.withStrikeoutness(true);
                    }
                    // add icons for various options
                    if (option.isRequired()) {
                        builder = builder.withIcon(AllIcons.Toolwindows.ToolWindowFavorites);
                    } else if (option.isSecret()) {
                        builder = builder.withIcon(AllIcons.Nodes.SecurityRole);
                    } else if (option.isMultiValue()) {
                        builder = builder.withIcon(AllIcons.General.ArrowRight);
                    } else if (option.getEnums() != null) {
                        builder = builder.withIcon(AllIcons.Nodes.Enum);
                    } else if ("object".equals(option.getType())) {
                        builder = builder.withIcon(AllIcons.Nodes.Class);
                    }

                    answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                }
            }
        }

        return answer;
    }


    @NotNull
    public static List<LookupElement> addSmartCompletionSuggestionsContextPath(String val,
                                                                               final ComponentModel component,
                                                                               final Map<String, String> existing,
                                                                               final PsiElement psiElement,
                                                                               final Predicate<ComponentModel> componentPredicate,
                                                                               final Predicate<ComponentModel.EndpointOptionModel> optionPredicate,
                                                                               final Function<ComponentModel.EndpointOptionModel, Icon> iconProvider) {
        final List<LookupElement> answer = new ArrayList<>();

        // show the syntax as the only choice for now
        LookupElementBuilder builder = LookupElementBuilder.create(val);
        builder = builder.withIcon(CamelPreferenceService.getService().getCamelIcon());
        builder = builder.withBoldness(true);
        builder = builder.withPresentableText(component.getSyntax());

        final LookupElement element = builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
        answer.add(element);
        val = removeUnknownEnum(val, psiElement);
        final List<LookupElement> old = addSmartCompletionContextPathSuggestions(
            val, component, existing, componentPredicate, optionPredicate, iconProvider
        );
        if (!old.isEmpty()) {
            answer.addAll(old);
        }

        return answer;
    }

    private static List<LookupElement> addSmartCompletionContextPathSuggestions(final String val,
                                                                                final ComponentModel component,
                                                                                final Map<String, String> existing,
                                                                                final Predicate<ComponentModel> componentPredicate,
                                                                                final Predicate<ComponentModel.EndpointOptionModel> optionPredicate,
                                                                                final Function<ComponentModel.EndpointOptionModel, Icon> iconProvider) {
        final List<LookupElement> answer = new ArrayList<>();
        if (componentPredicate.test(component)) {
            double priority = 100.0d;
            for (final ComponentModel.EndpointOptionModel option : component.getEndpointOptions()) {
                // only add support for enum in the context-path smart completion
                if ("path".equals(option.getKind()) && optionPredicate.test(option)) {
                    final String name = option.getName();
                    // only add if not already used
                    final String old = existing != null ? existing.get(name) : "";
                    if (existing == null || old == null || old.isEmpty()) {
                        List<String> enums = option.getEnums();
                        if (enums == null || enums.isEmpty()) {
                            priority = createContextPathLookupElement(
                                answer, priority, option, name, option.getDisplayName(), val + name, iconProvider
                            );
                        } else {
                            // add all enum as choices
                            for (final String choice : enums) {
                                priority = createContextPathLookupElement(
                                    answer, priority, option, name, choice, val + choice, iconProvider
                                );
                            }
                        }
                    }
                }
            }
        }

        return answer;
    }

    private static double createContextPathLookupElement(List<LookupElement> answer, double priority,
                                                         ComponentModel.EndpointOptionModel option, String name,
                                                         String choice, String lookup,
                                                         Function<ComponentModel.EndpointOptionModel, Icon> iconProvider) {
        LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, lookup));
        // only show the option in the UI
        builder = builder.withPresentableText(choice);
        // lets use the option name as the type so its visible
        builder = builder.withTypeText(name, true);
        builder = builder.withIcon(iconProvider.apply(option));

        if (option.isDeprecated()) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }

        // its an enum so always auto complete the choices
        LookupElement element = builder.withAutoCompletionPolicy(AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE);

        // they should be in the exact order
        element = PrioritizedLookupElement.withPriority(element, priority);

        priority -= 1.0d;

        answer.add(element);
        return priority;
    }

    /**
     * Remove unknown option at the cursor location from the query string
     * from("timer:trigger?repeatCount=10&del<caret>")
     */
    private static String removeUnknownOption(String val, final Map<String, String> existing,
                                              final PsiElement element) {

        final String[] strToRemove = IdeaUtils.getService().getQueryParameterAtCursorPosition(element);
        //to compare the string against known options we need to strip it from equal sign
        String searchStr = strToRemove[0];
        if (!searchStr.isEmpty() && !searchStr.endsWith("&") && existing != null) {
            if (searchStr.startsWith("&") || searchStr.startsWith("?")) {
                searchStr = searchStr.substring(1);
            }
            //check if the option is known option
            final String optionToRemove = existing.get(searchStr);
            if (optionToRemove == null || optionToRemove.isEmpty()) {
                val = val.replace(searchStr, "");
            }
        }
        return val;
    }

    /**
     * Remove unknown option at the cursor location from the query string
     * from("jms:qu<caret>")
     */
    private static String removeUnknownEnum(String val, final PsiElement element) {
        final String[] strToRemove = IdeaUtils.getService().getQueryParameterAtCursorPosition(element);
        //to compare the string against known options we need to strip it from equal sign
        strToRemove[0] = strToRemove[0].replace(":", "");
        if (!strToRemove[0].isEmpty()) {
            val = val.replace(strToRemove[0], "");
        }
        return val;
    }

    /**
     * We need special logic to determine when it should insert "=" at the end of the options
     */
    @NotNull
    private static LookupElementBuilder addInsertHandler(final Editor editor, final LookupElementBuilder builder,
                                                         final String suffix) {
        return builder.withInsertHandler((context, item) -> {
            // enforce using replace select char as we want to replace any existing option
            if (context.getCompletionChar() == Lookup.NORMAL_SELECT_CHAR) {
                int endSelectOffBy = 0;
                if (context.getFile() instanceof PropertiesFileImpl) {
                    //if it's a property file the PsiElement does not start and end with an quot
                    endSelectOffBy = 1;
                }
                final char text = context
                        .getDocument()
                        .getCharsSequence()
                        .charAt(context.getSelectionEndOffset() - endSelectOffBy);
                if (text != '=') {
                    EditorModificationUtil.insertStringAtCaret(editor, "=");
                }
            } else if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                // we still want to keep the suffix because they are other options
                String value = suffix;
                final int pos = value.indexOf("&");
                if (pos > -1) {
                    // strip out first part of suffix until next option
                    value = value.substring(pos);
                }
                EditorModificationUtil.insertStringAtCaret(editor, "=" + value);
                // and move cursor back again
                final int offset = -1 * value.length();
                EditorModificationUtil.moveCaretRelatively(editor, offset);
            }
        });
    }

}
