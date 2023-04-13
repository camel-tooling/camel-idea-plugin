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

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.tooling.model.ComponentModel;

/**
 * The {@link CompletionProvider} that gives the name of potential headers in case of Java files.
 */
public class CamelJavaHeaderNameCompletion extends CamelHeaderNameCompletion {

    /**
     * Indicates whether the method into which the name of header should be injected has other arguments.
     */
    private final boolean hasOtherArguments;

    /**
     * Constructs a {@code CamelJavaHeaderNameCompletion} with the given parameters.
     * @param source the source of endpoints from which we extract the name of headers to propose.
     * @param hasOtherArguments indicates whether the method into which the name of header should be injected has other
     *                          arguments.
     */
    public CamelJavaHeaderNameCompletion(CamelHeaderEndpointSource source, boolean hasOtherArguments) {
        super(source);
        this.hasOtherArguments = hasOtherArguments;
    }

    @Override
    protected String extractTextFromElement(final PsiElement element) {
        final PsiReferenceExpression ref = PsiTreeUtil.findChildOfType(
            element.getParent(), PsiReferenceExpression.class, false
        );
        if (ref != null) {
            return IdeaUtils.getService().extractTextFromElement(ref);
        }
        return null;
    }

    @Override
    protected boolean isStringLiteralExpected(final PsiElement element) {
        return element instanceof PsiJavaToken && ((PsiJavaToken) element).getTokenType() == JavaTokenType.STRING_LITERAL;
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilder(final PsiElement element,
                                                              final ComponentModel.EndpointHeaderModel header) {
        LookupElementBuilder builder;
        // only show the header in the UI
        final String constantName = header.getConstantName();
        final String name = header.getName();
        int index;
        if (isStringLiteralExpected(element)) {
            builder = LookupElementBuilder.create(new OptionSuggestion(header, name));
        } else if (constantName == null || (index = constantName.indexOf('#')) == -1) {
            builder = LookupElementBuilder.create(
                new OptionSuggestion(header, formatSuggestion(String.format("\"%s\"", name)))
            )
                .withLookupString(name)
                .withPresentableText(name);
        } else {
            final String className = constantName.substring(0, index);
            final String simpleConstant = String.format(
                "%s.%s", className.substring(className.lastIndexOf('.') + 1),
                constantName.substring(index + 1)
            );
            final int indexNameProvider = simpleConstant.indexOf('@');
            builder = LookupElementBuilder.create(new OptionSuggestion(
                header, formatSuggestion(simpleConstant.replace('@', '.')))
            )
                .withPresentableText(
                    indexNameProvider == -1 ? simpleConstant : simpleConstant.substring(0, indexNameProvider)
                )
                .withTailText(String.format(" ( = \"%s\")", header.getName()))
                .withInsertHandler(
                    new CamelJavaHeaderInsertHandler(
                        className, constantName.replace('#', '.').replace('@', '.'), 0
                    )
                );
        }
        return builder;
    }

    /**
     * Formats the given suggestion in order to adapt it to context (with or without other arguments).
     */
    private String formatSuggestion(String suggestion) {
        if (hasOtherArguments) {
            return String.format("%s, ", suggestion);
        }
        return suggestion;
    }
}
