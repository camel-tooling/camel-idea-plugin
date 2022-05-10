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

import com.github.cameltooling.idea.completion.extension.CamelEndpointNameCompletionExtension;
import com.github.cameltooling.idea.completion.extension.CamelEndpointSmartCompletionExtension;
import com.github.cameltooling.idea.completion.header.CamelHeaderEndpointSource;
import com.github.cameltooling.idea.completion.header.CamelYamlHeaderNameCompletion;
import com.github.cameltooling.idea.completion.header.CamelYamlHeaderValueCompletion;
import com.github.cameltooling.idea.util.YamlPatternConditions;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA completion system, to set up Camel smart completion for yaml files.
 */
public class CamelYamlFileReferenceContributor extends CamelContributor {

    public CamelYamlFileReferenceContributor() {
        addCompletionExtension(new CamelEndpointNameCompletionExtension());
        addCompletionExtension(new CamelEndpointSmartCompletionExtension(false));
        extend(CompletionType.BASIC,
            psiElement().and(psiElement().inside(PsiFile.class).inFile(matchFileType("yaml", "yml"))),
            new EndpointCompletion(getCamelCompletionExtensions())
        );
        final String[] setHeaderTagNames = {"set-header", "setHeader"};
        // The name of the header corresponding to the value of the key "name" in the dictionary
        // set-header or setHeader
        extend(CompletionType.BASIC,
            psiElement()
                .withParent(
                    psiElement()
                        .with(
                            YamlPatternConditions.withElementType(
                                YAMLElementTypes.SCALAR_PLAIN_VALUE, YAMLElementTypes.SCALAR_QUOTED_STRING
                            )
                        )
                        .withParent(
                            psiElement(YAMLKeyValue.class)
                                .with(
                                    YamlPatternConditions.withFirstChild(
                                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                            .withText("name")
                                    )
                                )
                                .withSuperParent(
                                    2,
                                    psiElement(YAMLKeyValue.class)
                                        .with(
                                            YamlPatternConditions.withFirstChild(
                                                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                    .with(
                                                        YamlPatternConditions.withText(setHeaderTagNames)
                                                    )
                                            )
                                        )
                                )
                        )
                )
            ,
            new CamelYamlHeaderNameCompletion(CamelHeaderEndpointSource.PRODUCER_ONLY)
        );
        // The value of the header corresponding to the value of the key "expression" in the dictionary
        // set-header or setHeader or to the pattern "expression:" (to be able to get the completion without the
        // space after the colon) in the dictionary set-header or setHeader
        //noinspection unchecked
        extend(CompletionType.BASIC,
            psiElement()
                .withParent(
                    psiElement()
                        .with(
                            YamlPatternConditions.withElementType(
                                YAMLElementTypes.SCALAR_PLAIN_VALUE, YAMLElementTypes.SCALAR_QUOTED_STRING
                            )
                        )
                        .with(
                            YamlPatternConditions.or(
                                psiElement()
                                    .withParent(
                                        psiElement(YAMLKeyValue.class)
                                            .with(
                                                YamlPatternConditions.withFirstChild(
                                                    PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                        .withText("expression")
                                                )
                                            ).withSuperParent(
                                                2,
                                                psiElement(YAMLKeyValue.class)
                                                    .with(
                                                        YamlPatternConditions.withFirstChild(
                                                            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                                .with(
                                                                    YamlPatternConditions.withText(setHeaderTagNames)
                                                                )
                                                        )
                                                    )
                                            )
                                    ),
                                    psiElement()
                                        .with(
                                            YamlPatternConditions.withFirstChild(
                                                PlatformPatterns.psiElement()
                                                    .withText(
                                                        String.format(
                                                            "expression:%s",
                                                            CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
                                                        )
                                                    )
                                            )
                                        )
                                        .withSuperParent(
                                            2,
                                            psiElement(YAMLKeyValue.class)
                                                .with(
                                                    YamlPatternConditions.withFirstChild(
                                                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                            .with(
                                                                YamlPatternConditions.withText(setHeaderTagNames)
                                                            )
                                                    )
                                                )
                                        )
                            )
                        )

                )
            ,
            new CamelYamlHeaderValueCompletion()
        );
        // The name of the header corresponding to the value of the key "header"
        extend(CompletionType.BASIC,
            psiElement()
                .withParent(
                    psiElement()
                        .with(
                            YamlPatternConditions.withElementType(
                                YAMLElementTypes.SCALAR_PLAIN_VALUE, YAMLElementTypes.SCALAR_QUOTED_STRING
                            )
                        )
                        .withParent(
                            psiElement(YAMLKeyValue.class)
                                .with(
                                    YamlPatternConditions.withFirstChild(
                                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                            .withText("header")
                                    )
                                )
                        )
                )
            ,
            new CamelYamlHeaderNameCompletion(CamelHeaderEndpointSource.ALL)
        );
    }
}
