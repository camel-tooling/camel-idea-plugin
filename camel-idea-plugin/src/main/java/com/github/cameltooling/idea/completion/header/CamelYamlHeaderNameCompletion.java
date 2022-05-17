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
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import org.apache.camel.tooling.model.ComponentModel;

/**
 * The {@link CompletionProvider} that gives the name of potential headers in case of YAML files.
 */
public class CamelYamlHeaderNameCompletion extends CamelHeaderNameCompletion {

    /**
     * Constructs a {@code CamelYamlHeaderNameCompletion} with the given source of endpoints.
     * @param source the source of endpoints from which we extract the name of headers to propose.
     */
    public CamelYamlHeaderNameCompletion(CamelHeaderEndpointSource source) {
        super(source);
    }

    @Override
    protected String extractTextFromElement(final PsiElement element) {
        return getIdeaUtils().extractTextFromElement(element);
    }

    @Override
    protected boolean isStringLiteralExpected(final PsiElement element) {
        return true;
    }

    @Override
    protected LookupElementBuilder createLookupElementBuilder(final PsiElement element,
                                                              final ComponentModel.EndpointHeaderModel header) {
        return LookupElementBuilder.create(new OptionSuggestion(header, header.getName()))
            .withInsertHandler(
                (context, item) -> {
                    final char text = context
                        .getDocument()
                        .getCharsSequence()
                        .charAt(context.getStartOffset() - 1);
                    if (text == ':') {
                        // If the last character is a colon we need to inject the missing space
                        context.getDocument().insertString(context.getStartOffset(), " ");
                    }
                }
            );
    }
}
