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
package org.apache.camel.idea.util;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

public final class IdeaUtils {

    private IdeaUtils() {
    }

    @Nullable
    public static String extractTextFromElement(PsiElement element) {
        // need the entire line so find the literal expression that would hold the entire string (java)
        PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression.class);
        if (literal != null) {
            Object o = literal.getValue();
            return o != null ? o.toString() : null;
        }

        // maybe its xml then try that
        XmlAttributeValue xml = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
        if (xml != null) {
            return xml.getValue();
        }

        // its maybe a property from properties file
        String fqn = element.getClass().getName();
        if (fqn.startsWith("com.intellij.lang.properties.psi.impl.PropertyValue")) {
            // yes we can support this also
            return element.getText();
        }

        // maybe its yaml
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("yaml")) {
                return element.getText();
            }
        }

        // maybe its groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                String text = element.getText();
                // unwrap groovy gstring
                return getInnerText(text);
            }
        }

        // maybe its scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                String text = element.getText();
                // unwrap scala string
                return getInnerText(text);
            }
        }

        // maybe its kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                String text = element.getText();
                // unwrap kotlin string
                return getKotlinInnerText(text);
            }
        }

        // fallback to generic
        return element.getText();
    }

    /**
     * Is the given element a string literal
     */
    @Deprecated
    public static boolean isStringLiteral(PsiElement element) {
        if (element instanceof PsiLiteralExpression) {
            PsiType type = ((PsiLiteralExpression) element).getType();
            String txt = type.getCanonicalText();
            return "java.lang.String".equals(txt);
        }
        return false;
    }

    /**
     * Is the given element a java token literal
     */
    @Deprecated
    public static boolean isJavaTokenLiteral(PsiElement element) {
        if (element instanceof PsiJavaToken) {
            PsiJavaToken token = (PsiJavaToken) element;
            IElementType type = token.getTokenType();
            if (type != null) {
                return "STRING_LITERAL".equals(type.toString());
            }
        }
        return false;
    }

    /**
     * Is the given element from a consumer endpoint used in a route from a <tt>from</tt>, <tt>fromF</tt>,
     * <tt>interceptFrom</tt>, or <tt>pollEnrich</tt> pattern.
     */
    public static boolean isConsumerEndpoint(PsiElement element) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            PsiMethod method = call.resolveMethod();
            if (method != null) {
                String name = method.getName();
                return "from".equals(name) || "fromF".equals(name) || "interceptFrom".equals(name) || "pollEnrich".equals(name);
            }
        }
        // annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Consume");
        }
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            String name = xml.getLocalName();
            // special check for poll enrich where we add the endpoint on a child node (camel expression)
            XmlTag parent = xml.getParentTag();
            if (parent != null && parent.getLocalName().equals("pollEnrich")) {
                return true;
            }
            return "from".equals(name) || "interceptFrom".equals(name);
        }

        return false;
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    public static String getInnerText(String text) {
        int textLength = text.length();
        if (StringUtil.endsWithChar(text, '\"')) {
            if (textLength == 1) {
                return null;
            }
            text = text.substring(1, textLength - 1);
        } else {
            if (text.startsWith(QUOT) && text.endsWith(QUOT) && textLength > QUOT.length()) {
                text = text.substring(QUOT.length(), textLength - QUOT.length());
            } else {
                return null;
            }
        }
        return text;
    }

    @Nullable
    public static String getKotlinInnerText(String text) {
        // it may be just a single quote
        int textLength = text.length();
        if (StringUtil.endsWithChar(text, '\"')) {
            if (textLength == 1) {
                // its a open or closing quote which kotlin breaks into two psi elements
                return "";
            }
        }
        return text;
    }
}
