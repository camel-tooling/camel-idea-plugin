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
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link CompletionProvider} that gives the potential values of a chosen header in case of XML files.
 */
public class CamelXmlHeaderValueCompletion extends CamelHeaderValueCompletion {

    @Override
    protected LookupElementBuilder createLookupElementBuilder(final PsiElement element, final String suggestion) {
        final PsiElement previous = element.getPrevSibling();
        String result = String.format("<constant>%s</constant>", suggestion);
        if (previous instanceof XmlTag) {
            XmlTag tag = (XmlTag) previous;
            if ("constant".startsWith(tag.getLocalName())) {
                result = result.substring(1 + tag.getLocalName().length());
            }
        }
        return LookupElementBuilder.create(result)
            .withPresentableText(suggestion);
    }

    @Override
    protected Predicate<ComponentModel.EndpointHeaderModel> predicate(@NotNull String headerName) {
        return header -> headerName.equals(header.getName());
    }

    @Override
    protected @Nullable String getHeaderName(@Nullable PsiElement element) {
        final XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (tag == null) {
            return null;
        }
        return tag.getAttributeValue("name");
    }

    @Override
    protected LookupElementBuilder createEnumLookupElementBuilder(final PsiElement element,
                                                                  final String suggestion,
                                                                  final String javaType) {
        return createLookupElementBuilder(element, suggestion);
    }

    @Override
    protected LookupElementBuilder createDefaultValueLookupElementBuilder(PsiElement element, String suggestion) {
        return createLookupElementBuilder(element, suggestion);
    }
}
