/**
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
package org.apache.camel.idea.completion.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.EndpointOptionModel;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.apache.camel.idea.util.IdeaUtils;
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

    public static List<LookupElement> addSmartCompletionSuggestionsQueryParameters(String[] query, ComponentModel component,
                                                                                   Map<String, String> existing, boolean xmlMode, PsiElement element, Editor editor) {
        List<LookupElement> answer = new ArrayList<>();

        boolean consumerOnly = getCamelIdeaUtils().isConsumerEndpoint(element);
        boolean producerOnly = getCamelIdeaUtils().isProducerEndpoint(element);

        String concatQuery = query[0];
        String suffix = query[1];
        String queryAtPosition = query[2];

        if (xmlMode) {
            queryAtPosition = queryAtPosition.replace("&amp;", "&");
        }

        List<EndpointOptionModel> options = component.getEndpointOptions();
        // sort the options A..Z which is easier to users to understand
        options.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        queryAtPosition = removeUnknownOption(queryAtPosition, existing, element);

        for (EndpointOptionModel option : options) {

            if ("parameter".equals(option.getKind())) {
                String name = option.getName();

                // if we are consumer only, then any option that has producer in the label should be skipped (as its only for producer)
                if (consumerOnly && option.getLabel().contains("producer")) {
                    continue;
                }
                // if we are producer only, then any option that has consume in the label should be skipped (as its only for consumer)
                if (producerOnly && option.getLabel().contains("consumer")) {
                    continue;
                }

                // only add if not already used (or if the option is multi valued then it can have many)
                String old = existing != null ? existing.get(name) : "";
                if ("true".equals(option.getMultiValue()) || existing == null || old == null || old.isEmpty()) {

                    // no tail for prefix, otherwise use = to setup for value
                    String key = option.getPrefix().isEmpty() ? name : option.getPrefix();

                    // the lookup should prepare for the new option
                    String lookup;
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
                    LookupElementBuilder builder = LookupElementBuilder.create(lookup);
                    builder = addInsertHandler(editor, builder, suffix);
                    // only show the option in the UI
                    builder = builder.withPresentableText(name);
                    // we don't want to highlight the advanced options which should be more seldom in use
                    boolean advanced = option.getGroup().contains("advanced");
                    builder = builder.withBoldness(!advanced);
                    if (!option.getJavaType().isEmpty()) {
                        builder = builder.withTypeText(option.getJavaType(), true);
                    }
                    if ("true".equals(option.getDeprecated())) {
                        // mark as deprecated
                        builder = builder.withStrikeoutness(true);
                    }
                    // add icons for various options
                    if ("true".equals(option.getRequired())) {
                        builder = builder.withIcon(AllIcons.Toolwindows.ToolWindowFavorites);
                    } else if ("true".equals(option.getSecret())) {
                        builder = builder.withIcon(AllIcons.Nodes.SecurityRole);
                    } else if ("true".equals(option.getMultiValue())) {
                        builder = builder.withIcon(AllIcons.Nodes.ExpandNode);
                    } else if (!option.getEnums().isEmpty()) {
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



    public static List<LookupElement> addSmartCompletionSuggestionsContextPath(String val, ComponentModel component,
                                                                               Map<String, String> existing, boolean xmlMode, PsiElement psiElement) {
        List<LookupElement> answer = new ArrayList<>();

        // show the syntax as the only choice for now
        LookupElementBuilder builder = LookupElementBuilder.create(val);
        builder = builder.withIcon(getCamelPreferenceService().getCamelIcon());
        builder = builder.withBoldness(true);
        builder = builder.withPresentableText(component.getSyntax());

        LookupElement element = builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
        answer.add(element);
        val = removeUnknownEnum(val, psiElement);
        List<LookupElement> old = addSmartCompletionContextPathEnumSuggestions(val, component, existing);
        if (!old.isEmpty()) {
            answer.addAll(old);
        }

        return answer;
    }

    private static List<LookupElement> addSmartCompletionContextPathEnumSuggestions(String val, ComponentModel component,
                                                                                    Map<String, String> existing) {
        List<LookupElement> answer = new ArrayList<>();

        double priority = 100.0d;

        // lets help the suggestion list if we are editing the context-path and only have 1 enum type option
        // and the option has not been in use yet, then we can populate the list with the enum values.

        long enums = component.getEndpointOptions().stream().filter(o -> "path".equals(o.getKind()) && !o.getEnums().isEmpty()).count();
        if (enums == 1) {
            for (EndpointOptionModel option : component.getEndpointOptions()) {

                // only add support for enum in the context-path smart completion
                if ("path".equals(option.getKind()) && !option.getEnums().isEmpty()) {
                    String name = option.getName();
                    // only add if not already used
                    String old = existing != null ? existing.get(name) : "";
                    if (existing == null || old == null || old.isEmpty()) {

                        // add all enum as choices
                        for (String choice : option.getEnums().split(",")) {

                            String key = choice;
                            String lookup = val + key;

                            LookupElementBuilder builder = LookupElementBuilder.create(lookup);
                            // only show the option in the UI
                            builder = builder.withPresentableText(choice);
                            // lets use the option name as the type so its visible
                            builder = builder.withTypeText(name, true);
                            builder = builder.withIcon(AllIcons.Nodes.Enum);

                            if ("true".equals(option.getDeprecated())) {
                                // mark as deprecated
                                builder = builder.withStrikeoutness(true);
                            }

                            // its an enum so always auto complete the choices
                            LookupElement element = builder.withAutoCompletionPolicy(AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE);

                            // they should be in the exact order
                            element = PrioritizedLookupElement.withPriority(element, priority);

                            priority -= 1.0d;

                            answer.add(element);
                        }
                    }
                }
            }
        }

        return answer;
    }

    /**
     * Remove unknown option at the cursor location from the query string
     * from("timer:trigger?repeatCount=10&del<caret>")
     */
    private static String removeUnknownOption(String val, Map<String, String> existing, PsiElement element) {

        String[] strToRemove = getIdeaUtils().getQueryParameterAtCursorPosition(element);
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
    private static String removeUnknownEnum(String val, PsiElement element) {

        String[] strToRemove = getIdeaUtils().getQueryParameterAtCursorPosition(element);
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
    private static LookupElementBuilder addInsertHandler(final Editor editor, final LookupElementBuilder builder, String suffix) {
        return builder.withInsertHandler((context, item) -> {
            // enforce using replace select char as we want to replace any existing option
            if (context.getCompletionChar() == Lookup.NORMAL_SELECT_CHAR) {
                int endSelectOffBy = 0;
                if (context.getFile() instanceof PropertiesFileImpl) {
                    //if it's a property file the PsiElement does not start and end with an quot
                    endSelectOffBy = 1;
                }
                final char text = context.getDocument().getCharsSequence().charAt(context.getSelectionEndOffset() - endSelectOffBy);
                if (text != '=') {
                    EditorModificationUtil.insertStringAtCaret(editor, "=");
                }
            } else if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                // we still want to keep the suffix because they are other options
                String value = suffix;
                int pos = value.indexOf("&");
                if (pos > -1) {
                    // strip out first part of suffix until next option
                    value = value.substring(pos);
                }
                EditorModificationUtil.insertStringAtCaret(editor, "=" + value);
                // and move cursor back again
                int offset = -1 * value.length();
                EditorModificationUtil.moveCaretRelatively(editor, offset);
            }

        });
    }

    private static CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

    private static IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

    private static CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

}
