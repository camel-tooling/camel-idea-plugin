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

import java.util.Optional;
import java.util.function.Predicate;

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.ComponentModel.EndpointHeaderModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * The {@link CompletionProvider} that gives the potential values of a chosen header in case of YAML files.
 */
public class CamelYamlHeaderValueCompletion extends CamelHeaderValueCompletion {

    @Override
    protected LookupElementBuilder createLookupElementBuilder(final PsiElement element,
                                                              final EndpointHeaderModel header,
                                                              final String suggestion) {
        return LookupElementBuilder.create(new OptionSuggestion(header, String.format("\nconstant: %s", suggestion)))
            .withLookupString(suggestion)
            .withPresentableText(suggestion)
            .withInsertHandler(
                (context, item) -> {
                    // Retrieve the content of the current line
                    final int startOffset = context.getStartOffset();
                    final int lineIndex = context.getDocument().getLineNumber(startOffset);
                    final CharSequence line = context.getDocument().getCharsSequence().subSequence(
                        context.getDocument().getLineStartOffset(lineIndex),
                        context.getDocument().getLineEndOffset(lineIndex)
                    );
                    if (line.toString().trim().isEmpty()) {
                        // Remove the empty line in case the value is not requested at the same level as expression
                        context.getDocument().deleteString(startOffset, startOffset + String.format("%n").length());
                    }
                    context.commitDocument();
                    // Adjust the indent to have the constant at the expected level
                    CodeStyleManager.getInstance(context.getProject()).adjustLineIndent(context.getFile(), startOffset);
                }
            );
    }

    @Override
    protected Predicate<ComponentModel.EndpointHeaderModel> predicate(@NotNull String headerName) {
        return header -> headerName.equals(header.getName());
    }

    @Override
    protected PsiElement getCompletionPosition(@NotNull CompletionParameters parameters) {
        return parameters.getPosition();
    }
    @Override
    protected @Nullable String getHeaderName(@Nullable PsiElement element) {
        final YAMLMapping mapping = PsiTreeUtil.getParentOfType(element, YAMLMapping.class);
        if (mapping == null) {
            return null;
        }
        return Optional.ofNullable(mapping.getKeyValueByKey("name"))
            .map(YAMLKeyValue::getValueText)
            .orElse(null);
    }

    @Override
    protected LookupElementBuilder createEnumLookupElementBuilder(final PsiElement element,
                                                                  final EndpointHeaderModel header,
                                                                  final String suggestion,
                                                                  final String javaType) {
        return createLookupElementBuilder(element, header, suggestion);
    }

    @Override
    protected LookupElementBuilder createDefaultValueLookupElementBuilder(final PsiElement element,
                                                                          final EndpointHeaderModel header,
                                                                          final String suggestion) {
        return createLookupElementBuilder(element, header, suggestion);
    }

    /**
     * If the position of the cursor is next to the colon of the key pattern then it keeps only what is after the
     * colon character otherwise it keeps entirely the default prefix.
     */
    @NotNull
    @Override
    protected String getPrefix(@NotNull PsiElement element, @NotNull String defaultPrefix) {
        return isNextToKeyPattern(element) ? defaultPrefix.substring("expression:".length()) : defaultPrefix;
    }

    /**
     * Indicates whether the position of the cursor is next to the colon of the key pattern.
     * @param element the element where the cursor is.
     * @return {@code true} if the cursor is next to the colon character, {@code false} otherwise.
     */
    private boolean isNextToKeyPattern(@NotNull PsiElement element) {
        return String.format(
            "expression:%s", CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
        ).equals(element.getText()) && element.getParent() != null
            && element.getParent().getParent() instanceof YAMLMapping;
    }
}
