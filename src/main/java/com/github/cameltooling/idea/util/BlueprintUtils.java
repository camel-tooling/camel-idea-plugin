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
package com.github.cameltooling.idea.util;

import com.github.cameltooling.idea.Constants;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

public class BlueprintUtils implements Disposable {

    public static final Pattern PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]*)}");
    public static final String PROPERTY_PLACEHOLDER_START_TAG = "${";
    public static final String PROPERTY_PLACEHOLDER_END_TAG = "}";

    public static BlueprintUtils getService() {
        return ApplicationManager.getApplication().getService(BlueprintUtils.class);
    }

    public @NotNull List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderLocations() {
        return List.of(
                XmlPatterns.xmlAttributeValue()
                        .inside(XmlPatterns.xmlTag().withNamespace(Constants.OSGI_BLUEPRINT_NAMESPACE))
                        .with(notCamelNamespace()),
                PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
                        .inside(XmlPatterns.xmlTag().withNamespace(Constants.OSGI_BLUEPRINT_NAMESPACE))
                        .with(notCamelNamespace())
        );
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

    public boolean isAllowedPropertyPlaceholderLocation(PsiElement element) {
        return getAllowedPropertyPlaceholderLocations().stream().anyMatch(pattern -> pattern.accepts(element));
    }

    public boolean isBlueprintNamespace(String namespace) {
        return namespace.contains(Constants.OSGI_BLUEPRINT_NAMESPACE);
    }

    /**
     * Checks if the given string contains an unclosed property placeholder. String must contain
     * the opening tag "${" not followed by the closing tag "}".
     */
    public boolean hasUnclosedPropertyPlaceholder(String value) {
        int startIndex = value.lastIndexOf(PROPERTY_PLACEHOLDER_START_TAG);
        int endIndex = value.lastIndexOf(PROPERTY_PLACEHOLDER_END_TAG);
        return startIndex >= 0 && endIndex < startIndex;
    }

    @Override
    public void dispose() {

    }
}
