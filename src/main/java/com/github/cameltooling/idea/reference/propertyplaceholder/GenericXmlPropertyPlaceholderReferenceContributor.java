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

import com.github.cameltooling.idea.Constants;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides references for property placeholders in XML files
 */
public class GenericXmlPropertyPlaceholderReferenceContributor extends AbstractPropertyPlaceholderReferenceContributor<XmlPropertyPlaceholderDefinition> {

    @Override
    protected List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderLocations() {
        return List.of(
                XmlPatterns.xmlAttributeValue()
                        .with(notCamelNamespace()),
                PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
                        .with(notCamelNamespace())
        );
    }

    @Override
    protected List<XmlPropertyPlaceholderDefinition> getPlaceholderDefinitions() {
        return CamelPreferenceService.getService().getEnabledXmlPropertyPlaceholders();
    }

    private @NotNull PatternCondition<PsiElement> notCamelNamespace() {
        return new PatternCondition<>("notCamelNamespace") {
            @Override
            public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
                XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, true);
                return tag != null && !Constants.CAMEL_BLUEPRINT_NAMESPACE.equals(tag.getNamespace());
            }
        };
    }

    @Override
    protected boolean isPlaceholderEnabledAt(XmlPropertyPlaceholderDefinition def, PsiElement location) {
        return XmlPatterns.xmlAttributeValue()
                .inside(XmlPatterns.xmlTag().withNamespace(def.getNamespaces().toArray(String[]::new)))
                .accepts(location);
    }

}
