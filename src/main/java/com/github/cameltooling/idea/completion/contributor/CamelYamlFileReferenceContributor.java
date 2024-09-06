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
import com.github.cameltooling.idea.completion.extension.CamelKameletNameCompletion;
import com.github.cameltooling.idea.completion.extension.CamelKameletOptionNameCompletion;
import com.github.cameltooling.idea.completion.extension.CamelKameletOptionValueCompletion;
import com.github.cameltooling.idea.completion.header.CamelHeaderEndpointSource;
import com.github.cameltooling.idea.completion.header.CamelYamlHeaderNameCompletion;
import com.github.cameltooling.idea.completion.header.CamelYamlHeaderValueCompletion;
import com.github.cameltooling.idea.completion.property.CamelYamlPropertyKeyCompletion;
import com.github.cameltooling.idea.completion.property.CamelYamlPropertyValueCompletion;
import com.github.cameltooling.idea.util.YamlPatternConditions;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA completion system, to set up Camel smart completion for yaml files.
 */
public class CamelYamlFileReferenceContributor extends CamelContributor {

    private static final PatternCondition<PsiElement> WITH_FIRST_CHILD_IS_PROPERTIES = YamlPatternConditions.withFirstChild(
        psiElement(YAMLTokenTypes.SCALAR_KEY)
            .withText("properties")
    );
    private static final PsiElementPattern.Capture<YAMLMapping> IS_KIND_KAMELET = psiElement(YAMLMapping.class)
        .withChild(
            psiElement(YAMLKeyValue.class)
                .with(
                    YamlPatternConditions.withFirstChild(
                        psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .withText("ref")
                    )
                )
                .withChild(
                    psiElement(YAMLMapping.class)

                        .withChild(
                            psiElement(YAMLKeyValue.class)
                                .with(
                                    YamlPatternConditions.withPair("kind", "Kamelet")
                                )
                        )
                        .withChild(
                            psiElement(YAMLKeyValue.class)
                                .with(
                                    YamlPatternConditions.withFirstChild(
                                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                            .withText("name")
                                    )
                                )
                        )
                )
        );

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
                ),
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

                ),
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
                ),
            new CamelYamlHeaderNameCompletion(CamelHeaderEndpointSource.ALL)
        );
        // The sub part of a property key corresponding to a text token whose parent is a scalar plain that doesn't
        // contain colon and whose grandparent is a document or a mapping in case of root level or has indent token
        // as previous sibling in case of non-root levels.
        extend(CompletionType.BASIC,
            psiElement(YAMLTokenTypes.TEXT)
                .withParent(
                    StandardPatterns.or(
                        psiElement(YAMLElementTypes.SCALAR_PLAIN_VALUE)
                            .withText(
                                StandardPatterns.not(StandardPatterns.string().contains(":"))
                            )
                            .withParent(
                                StandardPatterns.or(
                                    psiElement(YAMLElementTypes.DOCUMENT), psiElement(YAMLElementTypes.MAPPING)
                                )
                            ),
                        psiElement(YAMLElementTypes.SCALAR_PLAIN_VALUE)
                            .with(
                                YamlPatternConditions.withPrevSibling(psiElement(YAMLTokenTypes.INDENT))
                            )
                            .withText(
                                StandardPatterns.not(StandardPatterns.string().contains(":"))
                            )
                    )
                ),
            new CamelYamlPropertyKeyCompletion()
        );

        // The value of a property corresponding to a text token whose parent is a scalar plain with a white space
        // token as previous sibling
        extend(CompletionType.BASIC,
            psiElement(YAMLTokenTypes.TEXT)
                .withParent(
                    psiElement(YAMLElementTypes.SCALAR_PLAIN_VALUE)
                        .with(
                            YamlPatternConditions.withPrevSibling(psiElement(TokenType.WHITE_SPACE))
                        )
                ),
            new CamelYamlPropertyValueCompletion()
        );
        // Detect the name of a Kamelet in a Kamelet binding
        addCamelKameletNameCompletion("source", true);
        addCamelKameletNameCompletion("steps", false);
        addCamelKameletNameCompletion("sink", false);
        addCamelKameletOptionNameCompletion();
        addCamelKameletOptionValueCompletion();
    }

    private void addCamelKameletNameCompletion(String ancestorKeyName, boolean onlyConsumer) {
        extend(CompletionType.BASIC,
            psiElement(YAMLTokenTypes.TEXT)
                .withSuperParent(
                    2,
                    psiElement(YAMLKeyValue.class)
                        .with(
                            YamlPatternConditions.withFirstChild(
                                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                    .withText("name")
                            )
                        )
                        .withParent(
                            psiElement(YAMLMapping.class)
                                .withChild(
                                    PlatformPatterns.psiElement(YAMLKeyValue.class)
                                        .with(
                                            YamlPatternConditions.withPair("kind", "Kamelet")
                                        )
                                )
                        )
                )
                .withSuperParent(
                    4,
                    psiElement(YAMLKeyValue.class)
                        .with(
                            YamlPatternConditions.withFirstChild(
                                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                    .withText("ref")
                            )
                        )
                )
                .withSuperParent(
                    6,
                    StandardPatterns.or(
                        psiElement(YAMLKeyValue.class)
                            .with(
                                YamlPatternConditions.withFirstChild(
                                    PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                        .withText(ancestorKeyName)
                                )
                            ),
                        psiElement(YAMLSequenceItem.class)
                            .withSuperParent(
                                2,
                                psiElement(YAMLKeyValue.class)
                                    .with(
                                        YamlPatternConditions.withFirstChild(
                                            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                .withText(ancestorKeyName)
                                        )
                                    )
                            )
                    )

                ),
            new CamelKameletNameCompletion(onlyConsumer)
        );
    }

    private void addCamelKameletOptionNameCompletion() {
        extend(CompletionType.BASIC,
            psiElement()
                .with(
                    YamlPatternConditions.or(
                        psiElement(YAMLTokenTypes.TEXT)
                            .withSuperParent(
                                2,
                                PlatformPatterns.psiElement(YAMLKeyValue.class)
                                    .with(
                                        WITH_FIRST_CHILD_IS_PROPERTIES
                                    )
                            ),
                        psiElement(YAMLTokenTypes.INDENT)
                            .afterSibling(
                                PlatformPatterns.psiElement(YAMLKeyValue.class)
                                    .with(
                                        WITH_FIRST_CHILD_IS_PROPERTIES
                                    )
                            ),
                        psiElement(YAMLTokenTypes.TEXT)
                            .withSuperParent(
                                3,
                                PlatformPatterns.psiElement(YAMLKeyValue.class)
                                    .with(
                                        WITH_FIRST_CHILD_IS_PROPERTIES
                                    )
                            )
                    )
                )
                .withAncestor(
                    5,
                    StandardPatterns.or(
                        psiElement(YAMLKeyValue.class)
                            .with(
                                YamlPatternConditions.withFirstChild(
                                    PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                        .with(YamlPatternConditions.withText("source", "sink"))
                                )
                            )
                            .withChild(IS_KIND_KAMELET),
                        psiElement(YAMLSequenceItem.class)
                            .withSuperParent(
                                2,
                                psiElement(YAMLKeyValue.class)
                                    .with(
                                        YamlPatternConditions.withFirstChild(
                                            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                .withText("steps")
                                        )
                                    )
                            )
                            .withChild(IS_KIND_KAMELET)
                    )
                ),
            new CamelKameletOptionNameCompletion()
        );
    }


    private void addCamelKameletOptionValueCompletion() {
        extend(CompletionType.BASIC,
            psiElement()
                .withParent(
                    psiElement().with(
                        YamlPatternConditions.withElementType(
                            YAMLElementTypes.SCALAR_PLAIN_VALUE, YAMLElementTypes.SCALAR_QUOTED_STRING
                        )
                    )
                )
                .withAncestor(
                    4,
                    PlatformPatterns.psiElement(YAMLKeyValue.class)
                        .with(
                            WITH_FIRST_CHILD_IS_PROPERTIES
                        )
                )
                .withAncestor(
                    6,
                    StandardPatterns.or(
                        psiElement(YAMLKeyValue.class)
                            .with(
                                YamlPatternConditions.withFirstChild(
                                    PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                        .with(YamlPatternConditions.withText("source", "sink"))
                                )
                            )
                            .withChild(IS_KIND_KAMELET),
                        psiElement(YAMLSequenceItem.class)
                            .withSuperParent(
                                2,
                                psiElement(YAMLKeyValue.class)
                                    .with(
                                        YamlPatternConditions.withFirstChild(
                                            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                                .withText("steps")
                                        )
                                    )
                            )
                            .withChild(IS_KIND_KAMELET)
                    )
                ),
            new CamelKameletOptionValueCompletion()
        );
    }
}
