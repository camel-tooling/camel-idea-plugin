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
package org.apache.camel.idea.service.extension.idea;

import java.util.Arrays;
import java.util.Optional;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.idea.extension.IdeaUtilsExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import static com.intellij.xml.CommonXmlStrings.QUOT;

public class XmlIdeaUtils implements IdeaUtilsExtension {
    @Override
    public Optional<String> extractTextFromElement(PsiElement element, boolean concatString, boolean stripWhitespace) {
        // maybe its xml then try that
        if (element instanceof XmlAttributeValue) {
            return Optional.ofNullable(((XmlAttributeValue) element).getValue());
        } else if (element instanceof XmlText) {
            return Optional.ofNullable(((XmlText) element).getValue());
        } else if (element instanceof XmlToken) {
            // it may be a token which is a part of an combined attribute
            if (concatString) {
                XmlAttributeValue xml = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
                if (xml != null) {
                    return Optional.ofNullable(getInnerText(xml.getValue()));
                }
            } else {
                String returnText = element.getText();
                final PsiElement prevSibling = element.getPrevSibling();
                if (prevSibling != null && prevSibling.getText().equalsIgnoreCase("&amp;")) {
                    returnText = prevSibling.getText() + returnText;
                }
                return Optional.ofNullable(getInnerText(returnText));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isElementFromSetterProperty(@NotNull PsiElement element, @NotNull String setter) {
        // its maybe an XML property
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            boolean bean = isFromXmlTag(xml, "bean", "property");
            if (bean) {
                String key = xml.getAttributeValue("name");
                return setter.equals(key);
            }
        }
        return false;

    }

    @Override
    public boolean isExtensionEnabled() {
        return true;
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    public static String getInnerText(String text) {
        if (text == null) {
            return null;
        }
        if (StringUtil.endsWithChar(text, '\"') && text.length() == 1) {
            return "";
        }
        // Remove any newline feed + whitespaces + single + double quot to concat a split string
        return StringUtil.unquoteString(text.replace(QUOT, "\"")).replaceAll("(^\\n\\s+|\\n\\s+$|\\n\\s+)|(\"\\s*\\+\\s*\")|(\"\\s*\\+\\s*\\n\\s*\"*)", "");
    }

    /**
     * Is the given element from a XML tag with any of the given tag names
     *
     * @param xml  the xml tag
     * @param methods  xml tag names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    boolean isFromXmlTag(@NotNull XmlTag xml, @NotNull String... methods) {
        String name = xml.getLocalName();
        return Arrays.stream(methods).anyMatch(name::equals);
    }

}
