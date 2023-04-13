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

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;

/**
 * Smart completion for editing a single value in a Camel endpoint uri, such as
 * <tt>jms:queue?acknowledgementModeName=_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible values for the <tt>acknowledgementModeName</tt> option.
 */
public final class CamelSmartCompletionEndpointValue {

    private CamelSmartCompletionEndpointValue() {
    }

    public static List<LookupElement> addSmartCompletionForEndpointValue(Editor editor, String val, String suffix,
                                                                         ComponentModel.EndpointOptionModel option, PsiElement element, boolean xmlMode) {
        List<LookupElement> answer = new ArrayList<>();

        String javaType = option.getJavaType();
        boolean deprecated = option.isDeprecated();
        List<String> enums = option.getEnums();
        Object defaultValue = option.getDefaultValue();
        String[] stringToRemove = IdeaUtils.getService().getQueryParameterAtCursorPosition(element);
        if (stringToRemove[1] != null && !stringToRemove[1].isEmpty()) {
            val = val.replace(stringToRemove[1], "");
        }
        if (enums != null) {
            addEnumSuggestions(editor, option, val, suffix, answer, deprecated, enums, defaultValue, xmlMode);
        } else if ("java.lang.Boolean".equals(javaType) || "boolean".equals(javaType)) {
            addBooleanSuggestions(editor, option, val, suffix, answer, deprecated, defaultValue, xmlMode);
        } else if (defaultValue != null) {
            // for any other kind of type and if there is a default value then add that as a suggestion
            // so its easy to see what the default value is
            addDefaultValueSuggestions(editor, option, val, suffix, answer, deprecated, defaultValue, xmlMode);
        }

        return answer;
    }

    private static void addEnumSuggestions(Editor editor, ComponentModel.EndpointOptionModel option, String val,
                                           String suffix, List<LookupElement> answer, boolean deprecated,
                                           List<String> enums, Object defaultValue, boolean xmlMode) {
        for (String part : enums) {
            String lookup = val + part;
            LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, lookup));
            builder = addInsertHandler(editor, suffix, builder, xmlMode);

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
                answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
            } else {
                answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
            }
        }
    }

    private static void addBooleanSuggestions(Editor editor, ComponentModel.EndpointOptionModel option, String val,
                                              String suffix, List<LookupElement> answer, boolean deprecated,
                                              Object defaultValue, boolean xmlMode) {
        // for boolean types then give a choice between true|false
        String lookup = val + "true";
        LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, lookup));
        builder = addInsertHandler(editor, suffix, builder, xmlMode);
        // only show the option in the UI
        builder = builder.withPresentableText("true");
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        boolean isDefaultValue = "true".equals(defaultValue);
        if (isDefaultValue) {
            builder = builder.withTailText(" (default value)");
            // add default value first in the list
            answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        } else {
            answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        }

        lookup = val + "false";
        builder = LookupElementBuilder.create(new OptionSuggestion(option, lookup));
        builder = addInsertHandler(editor, suffix, builder, xmlMode);
        // only show the option in the UI
        builder = builder.withPresentableText("false");
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        isDefaultValue = "false".equals(defaultValue);
        if (isDefaultValue) {
            builder = builder.withTailText(" (default value)");
            // add default value first in the list
            answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        } else {
            answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        }
    }

    private static void addDefaultValueSuggestions(Editor editor, ComponentModel.EndpointOptionModel option, String val,
                                                   String suffix, List<LookupElement> answer, boolean deprecated,
                                                   Object defaultValue, boolean xmlMode) {
        String lookup = val + defaultValue;
        LookupElementBuilder builder = LookupElementBuilder.create(new OptionSuggestion(option, lookup));
        builder = addInsertHandler(editor, suffix, builder, xmlMode);
        // only show the option in the UI
        if (defaultValue != null) {
            builder = builder.withPresentableText(defaultValue.toString());
        }
        if (deprecated) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        builder = builder.withTailText(" (default value)");
        // there is only one value in the list and its the default value, so never auto complete it but show as suggestion
        answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE));
    }

    /**
     * We need special logic to preserve the suffix when the user selects an option to be inserted
     * and also to remove the old value when pressing enter to a choice
     */
    @NotNull
    private static LookupElementBuilder addInsertHandler(final Editor editor, final String suffix, final LookupElementBuilder builder, boolean xmlMode) {
        return builder.withInsertHandler((context, item) -> {
            // enforce using replace select char as we want to replace any existing option
            int pos;
            if (xmlMode) {
                pos = suffix.indexOf("&amp;");
            } else {
                pos = suffix.indexOf("&");
            }

            if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                // we still want to keep the suffix because they are other options
                String value = suffix;
                if (pos > -1) {
                    // strip out first part of suffix until next option
                    value = value.substring(pos);
                }
                EditorModificationUtil.insertStringAtCaret(editor, value);
                // and move cursor back again
                int offset = -1 * value.length();
                EditorModificationUtil.moveCaretRelatively(editor, offset);
            } else if (context.getCompletionChar() == Lookup.NORMAL_SELECT_CHAR) {
                // we want to remove the old option (which is the first value in the suffix)
                String cut = suffix;
                if (pos > 0) {
                    cut = cut.substring(0, pos);
                }
                int len = cut.length();
                if (len > 0 && pos != 0) {
                    int offset = editor.getCaretModel().getOffset();
                    editor.getDocument().deleteString(offset, offset + len);
                }
            }
        });
    }

}
