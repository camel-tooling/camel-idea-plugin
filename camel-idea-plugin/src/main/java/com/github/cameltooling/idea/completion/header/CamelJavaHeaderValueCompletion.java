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

import java.util.function.Predicate;

import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link CompletionProvider} that gives the potential values of a chosen header in case of Java files.
 */
public class CamelJavaHeaderValueCompletion extends CamelHeaderValueCompletion {

    /**
     * The prefix to add to the suggestion.
     */
    private static final String PREFIX = "constant(";
    @Override
    protected LookupElementBuilder createLookupElementBuilder(final PsiElement element, final String suggestion) {
        return LookupElementBuilder.create(String.format("%s%s)", PREFIX, suggestion))
            .withLookupString(suggestion)
            .withPresentableText(suggestion);
    }

    @Override
    protected Predicate<ComponentModel.EndpointHeaderModel> predicate(@NotNull String headerName) {
        final int index = headerName.lastIndexOf('.');
        if (index == -1) {
            return header -> headerName.equals(header.getName());
        }
        final String constantName = headerName.substring(0, index) + "#" + headerName.substring(index + 1);
        return header -> header.getConstantName().contains(constantName);
    }

    @Override
    protected @Nullable String getHeaderName(@Nullable PsiElement element) {
        final PsiExpressionList expressionList = PsiTreeUtil.getParentOfType(element, PsiExpressionList.class);
        if (expressionList == null || expressionList.isEmpty()) {
            return null;
        }
        return StringUtil.unquoteString(expressionList.getExpressions()[0].getText());
    }

    @Override
    protected LookupElementBuilder createEnumLookupElementBuilder(final PsiElement element,
                                                                  final String suggestion,
                                                                  final String javaType) {

        return createLookupElementBuilder(
            element, String.format("%s.%s", javaType.substring(javaType.lastIndexOf('.') + 1), suggestion)
        )
            .withPresentableText(suggestion)
            .withLookupString(suggestion)
            .withInsertHandler(
                new CamelJavaHeaderInsertHandler(
                    javaType, String.format("%s.%s", javaType, suggestion), PREFIX.length()
                )
            );
    }

    @Override
    protected LookupElementBuilder createDefaultValueLookupElementBuilder(PsiElement element, String suggestion) {
        return createLookupElementBuilder(element, String.format("\"%s\"", suggestion))
            .withPresentableText(suggestion)
            .withLookupString(suggestion);
    }
}
