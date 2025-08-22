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
package com.github.cameltooling.idea.reference.propertyplaceholder;

import com.github.cameltooling.idea.reference.CamelPsiReferenceProvider;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * References from the value of @ConfigProperty annotation to the property in properties files
 */
public class ConfigPropertyPlaceholderReferenceContributor extends PsiReferenceContributor {

    private static final String ANNOTATION_BLUEPRINT_CONFIG_PROPERTY = "org.apache.aries.blueprint.annotation.config.ConfigProperty";
    private static final PsiJavaElementPattern.Capture<PsiLiteralExpression> PATTERN = PsiJavaPatterns.literalExpression()
            .insideAnnotationParam(StandardPatterns.string()
                            .equalTo(ANNOTATION_BLUEPRINT_CONFIG_PROPERTY),
                    "value");

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PATTERN, new CamelPsiReferenceProvider() {

            @Override
            protected PsiReference[] getCamelReferencesByElement(PsiElement element, ProcessingContext context) {
                String origText = element.getText();
                String value = StringUtil.unquoteString(origText);
                if (value.startsWith("${") && value.endsWith("}")) {
                    int startOffset = origText.startsWith("\"") ? 3 : 2;
                    String propertyKey = value.substring(2, value.length() - 1);
                    var textRange = new TextRange(startOffset, startOffset + propertyKey.length());
                    return new PsiReference[] {
                            new PropertyReference(propertyKey, element, null, true, textRange)
                    };
                } else {
                    // we do not want the default PropertyReference IDEA places on all string literals to be present,
                    // because it would work and could lead user to think it's the proper syntax. So let's place
                    // a fake reference that is not resolvable.
                    return new PsiReference[] {
                            new NonResolvableReference(element)
                    };
                }
            }
        }, PsiReferenceRegistrar.HIGHER_PRIORITY); // IDEA offers its own placeholder reference to the whole string, including ${ and }, so we make our more important
    }

    private static class NonResolvableReference extends PsiReferenceBase<PsiElement> {

        private NonResolvableReference(PsiElement element) {
            super(element);
        }

        @Override
        public @Nullable PsiElement resolve() {
            return null;
        }

    }

}
