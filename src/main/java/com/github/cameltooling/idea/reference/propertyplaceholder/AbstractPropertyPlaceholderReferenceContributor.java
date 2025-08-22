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
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Provides references for property placeholders
 * Using native IDEA {@link PropertyReference} references automatically enables renaming functionality.
 */
public abstract class AbstractPropertyPlaceholderReferenceContributor<T extends PropertyPlaceholderDefinition> extends PsiReferenceContributor {

    protected abstract List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderLocations();
    protected abstract List<T> getPlaceholderDefinitions();

    protected boolean isPlaceholderEnabledAt(T def, PsiElement location) {
        return true;
    }

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        List<ElementPattern<? extends PsiElement>> patterns = getAllowedPropertyPlaceholderLocations();
        if (!patterns.isEmpty()) {
            PsiReferenceProvider propertyReferenceProvider = createProvider();
            patterns.forEach(pattern -> {
                registrar.registerReferenceProvider(pattern, propertyReferenceProvider);
            });
        }
    }

    @NotNull
    private PsiReferenceProvider createProvider() {
        return new CamelPsiReferenceProvider() {
            @Override
            public PsiReference[] getCamelReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                String text = element.getText();

                List<PsiReference> references = new ArrayList<>();

                for (T def : getPlaceholderDefinitions()) {
                    if (!isPlaceholderEnabledAt(def, element)) {
                        continue;
                    }

                    Matcher matcher = def.getPattern().matcher(text);

                    while (matcher.find()) {
                        PropertyReference propertyReference = getPropertyReference(def, element, matcher);
                        references.add(propertyReference);
                    }
                }

                return references.toArray(new PsiReference[0]);
            }

            private @NotNull PropertyReference getPropertyReference(T def, @NotNull PsiElement element, Matcher matcher) {
                String propertyName = matcher.group(1);
                int startOffset = matcher.start() + def.getStartToken().length();
                int endOffset = startOffset + propertyName.length();

                TextRange textRange = new TextRange(startOffset, endOffset);
                return new PropertyPlaceholderBasedPropertyReference(
                        def,
                        propertyName,
                        element,
                        null,
                        true,
                        textRange
                );
            }
        };
    }

}