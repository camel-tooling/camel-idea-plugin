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
package com.github.cameltooling.idea.service.extension.idea;

import java.util.Optional;

import com.github.cameltooling.idea.extension.IdeaUtilsExtension;
import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

public class YamlIdeaUtils implements IdeaUtilsExtension {

    @Override
    public Optional<String> extractTextFromElement(@NotNull PsiElement element, boolean concatString,
                                                   boolean stripWhitespace) {
        // maybe its yaml
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("yaml")) {
                final String text = getInnerText(element.getText());
                if (text == null || concatString) {
                    return Optional.ofNullable(text);
                }
                return extractCompletionPositionTextOnly(text);
            }
        }
        return Optional.empty();
    }

    /**
     * Gives the subsection of the given text that contains the token representing the position
     * where the completion is requested knowing that the character {@code &} is a separator.
     * @param text the text from which the extract the sub-part with the token representing
     *             the position where the completion is requested.
     * @return the entire string if the token could not be found or no separator could be found,
     * everything after the separator (included) located before the token if there is no separator located after
     * the token, everything before the separator located after the token if there is no separator located before
     * the token, everything between the separators located before and after the token otherwise.
     */
    private @NotNull Optional<String> extractCompletionPositionTextOnly(@NotNull String text) {
        int position = text.indexOf(CompletionInitializationContext.DUMMY_IDENTIFIER);
        if (position == -1) {
            return Optional.of(text);
        }
        int beforeIndex = text.substring(0, position).lastIndexOf('&');
        int afterIndex = text.indexOf('&', position);
        if (beforeIndex == -1 && afterIndex == -1) {
            return Optional.of(text);
        } else if (afterIndex == -1) {
            return Optional.of(text.substring(beforeIndex));
        } else if (beforeIndex == -1) {
            return Optional.of(text.substring(0, afterIndex));
        }
        return Optional.of(text.substring(beforeIndex, afterIndex));
    }

    @Override
    public boolean isElementFromSetterProperty(@NotNull PsiElement element, @NotNull String setter) {
        return false;
    }

    /**
     * @param editor The editor for which the indent must be returned.
     * @param offset the offset at which the indent is expected
     * @param indentTimesMore the total amount of times a new indent should be added to the initial indent.
     * @return the indent computed from the indent used at the caret offset to which the additional indent is applied.
     */
    public static String getIndent(Editor editor, int offset, int indentTimesMore) {
        // Collect the line indent of the current line
        String indent = CodeStyle.getLineIndent(editor, Language.findLanguageByID("yaml"), offset, false);
        if (indent == null) {
            indent = "";
        }
        return indent + "  ".repeat(Math.max(0, indentTimesMore));
    }

    @Override
    public boolean isExtensionEnabled() {
        final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.plugins.yaml"));
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    private static String getInnerText(String text) {
        if (text == null) {
            return null;
        }
        if (StringUtil.endsWithChar(text, '\"') && text.length() == 1) {
            return "";
        }
        // Remove any newline feed + whitespaces + single + double quot to concat a split string
        return StringUtil.unquoteString(text.replace(QUOT, "\"")).replaceAll("(^\\n\\s+|\\n\\s+$|\\n\\s+)|(\"\\s*\\+\\s*\")|(\"\\s*\\+\\s*\\n\\s*\"*)", "");
    }
}
