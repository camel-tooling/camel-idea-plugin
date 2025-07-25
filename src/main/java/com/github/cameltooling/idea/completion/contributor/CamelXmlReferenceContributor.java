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
package com.github.cameltooling.idea.completion.contributor;

import com.github.cameltooling.idea.completion.extension.BeanReferenceCompletionExtension;
import com.github.cameltooling.idea.completion.extension.BlueprintPropertyNameCompletionExtension;
import com.github.cameltooling.idea.completion.extension.CamelEndpointNameCompletionExtension;
import com.github.cameltooling.idea.completion.extension.CamelEndpointSmartCompletionExtension;
import com.github.cameltooling.idea.completion.extension.CamelPropertyPlaceholderSmartCompletionExtension;
import com.github.cameltooling.idea.completion.header.CamelHeaderEndpointSource;
import com.github.cameltooling.idea.completion.header.CamelXmlHeaderNameCompletion;
import com.github.cameltooling.idea.completion.header.CamelXmlHeaderValueCompletion;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlText;

import static com.github.cameltooling.idea.Constants.CAMEL_NAMESPACE;
import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA XML language, to set up Camel smart completion for editing XML source code.
 */
public class CamelXmlReferenceContributor extends CamelContributor {

    public CamelXmlReferenceContributor() {
        addCompletionExtension(new BeanReferenceCompletionExtension());
        addCompletionExtension(new BlueprintPropertyNameCompletionExtension());
        addCompletionExtension(new CamelEndpointNameCompletionExtension());
        addCompletionExtension(new CamelEndpointSmartCompletionExtension(true));
        addCompletionExtension(new CamelPropertyPlaceholderSmartCompletionExtension());
        extend(CompletionType.BASIC,
                psiElement().and(psiElement().inside(PsiFile.class).inFile(matchFileType("xml"))),
                new CompositeCompletionProvider(getCamelCompletionExtensions())
        );
        final String setHeaderTagName = "setHeader";
        // The name of the header corresponding to the attribute "name" of the tag "setHeader"
        extend(CompletionType.BASIC,
            psiElement().withParent(
                psiElement(XmlAttributeValue.class).withParent(
                    XmlPatterns.xmlAttribute().withName("name").withParent(
                        XmlPatterns.xmlTag()
                            .withName(setHeaderTagName)
                            .withNamespace(CAMEL_NAMESPACE)
                    )
                )
            ),
            new CamelXmlHeaderNameCompletion(CamelHeaderEndpointSource.PRODUCER_ONLY)
        );
        // The value of the header corresponding to the text content of the tag "setHeader" or the staring tag
        // inside the tag "setHeader"
        extend(CompletionType.BASIC,
            StandardPatterns.or(
                psiElement().withParent(
                    psiElement(XmlText.class).withParent(
                        XmlPatterns.xmlTag()
                            .withName(setHeaderTagName)
                            .withNamespace(CAMEL_NAMESPACE)
                    )
                ),
                psiElement().withParent(
                    XmlPatterns.xmlTag().withParent(
                        XmlPatterns.xmlTag()
                            .withName(setHeaderTagName)
                            .withNamespace(CAMEL_NAMESPACE)
                    )
                )
            ),
            new CamelXmlHeaderValueCompletion()
        );
        // The name of the header corresponding to the text content of the tag "header"
        extend(CompletionType.BASIC,
            psiElement().withParent(
                psiElement(XmlText.class).withParent(
                    XmlPatterns.xmlTag()
                        .withName("header")
                        .withNamespace(CAMEL_NAMESPACE)
                )
            ),
            new CamelXmlHeaderNameCompletion(CamelHeaderEndpointSource.ALL)
        );
    }

}
