/**
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
package org.apache.camel.idea.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.JavaDocElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlElementType;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.IdeaUtils;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;

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
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent() && isEnabled()) {
            boolean accept = accept(element);
            if (accept) {
                String text = getIdeaUtils().extractTextFromElement(element, true, false, false);
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
        } else {
            final PsiPolyadicExpression parentOfType = PsiTreeUtil.getParentOfType(element, PsiPolyadicExpression.class);
            if (((element instanceof PsiLiteralExpression) || (element instanceof PropertyValueImpl)) && (parentOfType == null)) {
                return true;
            }
        }

        // skip whitespace noise
        IElementType type = element.getNode().getElementType();
        if (type == TokenType.WHITE_SPACE) {
            return false;
        }

        // skip java doc noise
        if (JavaDocElementType.ALL_JAVADOC_ELEMENTS.contains(type)) {
            return false;
        }

        boolean accept = false;

        // we only want xml attributes or text value elements
        if (element instanceof XmlElement) {
            accept = type == XmlElementType.XML_ATTRIBUTE_VALUE
                || type == XmlElementType.XML_TEXT;
        }

        return accept;
    }

    /**
     * Validate the text and create error messaged from the validation result.
     *
     * @param element - Element to parse
     * @param holder - Container for the different error messages and it's test range
     * @param text - String to validate such as an Camel endpoint uri, or a Simple expression
     */
    abstract void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text);

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

}
