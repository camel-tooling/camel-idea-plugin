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
package com.github.cameltooling.idea.annotator;

import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.JavaDocElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * Validate if the URI contains a know Camel component and call the validateEndpoint method
 */
abstract class AbstractCamelAnnotator implements Annotator {

    /**
     * Whether or not the annotator is enabled.
     * <p/>
     * The user can turn this on or off in the plugin preference.
     */
    abstract boolean isEnabled();

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element.getProject().getService(CamelService.class).isCamelProject() && isEnabled()) {
            boolean accept = accept(element);
            if (accept) {
                String text = IdeaUtils.getService().extractTextFromElement(element, true, false, false);
                if (!StringUtils.isEmpty(text)) {
                    validateText(element, holder, text);
                }
            }
        }
    }

    /**
     * To filter unwanted elements
     *
     * @return <ttt>true</ttt> to accept or <tt>false</tt> to drop
     */
    boolean accept(PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }

        if (element instanceof PsiPolyadicExpression) {
            return true;
        }

        PsiPolyadicExpression parentOfType = PsiTreeUtil.getParentOfType(element, PsiPolyadicExpression.class);

        // Simplified the complex conditional
        if (isLiteralOrPropertyValueWithoutParent(element, parentOfType)) {
            return true;
        }

        // Skip whitespace and JavaDoc noise
        IElementType type = element.getNode().getElementType();
        if (type == TokenType.WHITE_SPACE || JavaDocElementType.ALL_JAVADOC_ELEMENTS.contains(type)) {
            return false;
        }

        // Check for YAML key-value pairs
        if (element instanceof YAMLKeyValue || element.getParent() instanceof YAMLKeyValue) {
            return true;
        }

        // Check for XML attributes or text value elements
        if (element instanceof XmlElement) {
            return type == XmlElementType.XML_ATTRIBUTE_VALUE || type == XmlElementType.XML_TEXT;
        }

        return false;
    }

    // Helper method for the complex conditional
    private boolean isLiteralOrPropertyValueWithoutParent(PsiElement element, PsiPolyadicExpression parentOfType) {
        return (element instanceof PsiLiteralExpression || element instanceof PropertyValueImpl) && parentOfType == null;
    }

    /**
     * Validate the text and create error messaged from the validation result.
     *
     * @param element - Element to parse
     * @param holder - Container for the different error messages and it's test range
     * @param text - String to validate such as an Camel endpoint uri, or a Simple expression
     */
    abstract void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text);

}
