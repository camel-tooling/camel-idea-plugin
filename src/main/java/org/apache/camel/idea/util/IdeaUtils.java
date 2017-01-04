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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
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

/**
 * Utility methods to work with IDEA {@link PsiElement}s.
 */
public final class IdeaUtils {

    private IdeaUtils() {
    }

    /**
     * Extract the text value from the {@link PsiElement} from any of the support languages this plugin works with.
     *
     * @param element the element
     * @return the text or <tt>null</tt> if the element is not a text/literal kind.
     */
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
            if (type != null) {
                String txt = type.getCanonicalText();
                return "java.lang.String".equals(txt);
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
        // groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                // need to walk a bit into the psi tree to find the element that holds the method call name
                // must be a groovy string kind
                String kind = element.toString();
                if (kind.contains("Gstring")) {
                    PsiElement parent = element.getParent();
                    if (parent != null) {
                        parent = parent.getParent();
                    }
                    if (parent != null) {
                        element = parent.getPrevSibling();
                    }
                    if (element != null) {
                        element = element.getLastChild();
                    }
                    if (element != null) {
                        kind = element.toString();
                        // must be an identifier which is part of the method call
                        if (kind.contains("identifier")) {
                            String name = element.getText();
                            return "from".equals(name) || "fromF".equals(name) || "interceptFrom".equals(name) || "pollEnrich".equals(name);
                        }
                    }
                }
                return false;
            }
        }
        // kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                // need to walk a bit into the psi tree to find the element that holds the method call name
                // (yes we need to go up till 6 levels up to find the method call expression
                String kind = element.toString();
                // must be a string kind
                if (kind.contains("STRING")) {
                    for (int i = 0; i < 6; i++) {
                        if (element != null) {
                            kind = element.toString();
                            if ("CALL_EXPRESSION".equals(kind)) {
                                element = element.getFirstChild();
                                if (element != null) {
                                    String name = element.getText();
                                    return "from".equals(name) || "fromF".equals(name) || "interceptFrom".equals(name) || "pollEnrich".equals(name);
                                }
                            }
                            if (element != null) {
                                element = element.getParent();
                            }
                        }
                    }
                }
                return false;
            }
        }
        // scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                // need to walk a bit into the psi tree to find the element that holds the method call name
                // (yes we need to go up till 5 levels up to find the method call expression
                String kind = element.toString();
                // must be a string kind
                if (kind.contains("string")) {
                    for (int i = 0; i < 5; i++) {
                        if (element != null) {
                            kind = element.toString();
                            if ("MethodCall".equals(kind)) {
                                element = element.getFirstChild();
                                if (element != null) {
                                    String name = element.getText();
                                    return "from".equals(name) || "fromF".equals(name) || "interceptFrom".equals(name) || "pollEnrich".equals(name);
                                }
                            }
                            if (element != null) {
                                element = element.getParent();
                            }
                        }
                    }
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Is the given element from a producer endpoint used in a route from a <tt>to</tt>, <tt>toF</tt>,
     * <tt>interceptSendToEndpoint</tt>, <tt>wireTap</tt>, or <tt>enrich</tt> pattern.
     */
    public static boolean isProducerEndpoint(PsiElement element) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            PsiMethod method = call.resolveMethod();
            if (method != null) {
                String name = method.getName();
                return "to".equals(name) || "toF".equals(name) || "toD".equals(name)
                    || "interceptSendToEndpoint".equals(name) || "enrich".equals(name) || "wireTap".equals(name);
            }
        }
        // annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Produce");
        }
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            String name = xml.getLocalName();
            // special check for enrich where we add the endpoint on a child node (camel expression)
            XmlTag parent = xml.getParentTag();
            if (parent != null && parent.getLocalName().equals("enrich")) {
                return true;
            }
            return "to".equals(name) || "interceptSendToEndpoint".equals(name) || "wireTap".equals(name);
        }
        // groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                // need to walk a bit into the psi tree to find the element that holds the method call name
                // must be a groovy string kind
                String kind = element.toString();
                if (kind.contains("Gstring")) {
                    PsiElement parent = element.getParent();
                    if (parent != null) {
                        parent = parent.getParent();
                    }
                    if (parent != null) {
                        element = parent.getPrevSibling();
                    }
                    if (element != null) {
                        element = element.getLastChild();
                    }
                    if (element != null) {
                        kind = element.toString();
                        // must be an identifier which is part of the method call
                        if (kind.contains("identifier")) {
                            String name = element.getText();
                            return "to".equals(name) || "toF".equals(name) || "toD".equals(name)
                                || "interceptSendToEndpoint".equals(name) || "enrich".equals(name) || "wireTap".equals(name);
                        }
                    }
                }
                return false;
            }
        }
        // kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                // need to walk a bit into the psi tree to find the element that holds the method call name
                // (yes we need to go up till 6 levels up to find the method call expression
                String kind = element.toString();
                // must be a string kind
                if (kind.contains("STRING")) {
                    for (int i = 0; i < 6; i++) {
                        if (element != null) {
                            kind = element.toString();
                            if ("CALL_EXPRESSION".equals(kind)) {
                                element = element.getFirstChild();
                                if (element != null) {
                                    String name = element.getText();
                                    return "to".equals(name) || "toF".equals(name) || "toD".equals(name)
                                        || "interceptSendToEndpoint".equals(name) || "enrich".equals(name) || "wireTap".equals(name);
                                }
                            }
                            if (element != null) {
                                element = element.getParent();
                            }
                        }
                    }
                }
                return false;
            }
        }
        // scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                // need to walk a bit into the psi tree to find the element that holds the method call name
                // (yes we need to go up till 5 levels up to find the method call expression
                String kind = element.toString();
                // must be a string kind
                if (kind.contains("string")) {
                    for (int i = 0; i < 5; i++) {
                        if (element != null) {
                            kind = element.toString();
                            if ("MethodCall".equals(kind)) {
                                element = element.getFirstChild();
                                if (element != null) {
                                    String name = element.getText();
                                    return "to".equals(name) || "toF".equals(name) || "toD".equals(name)
                                        || "interceptSendToEndpoint".equals(name) || "enrich".equals(name) || "wireTap".equals(name);
                                }
                            }
                            if (element != null) {
                                element = element.getParent();
                            }
                        }
                    }
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Is the class a Camel expression class
     *
     * @param clazz  the class
     * @return <tt>true</tt> if its a Camel expression class, <tt>false</tt> otherwise.
     */
    public static boolean isCamelExpressionOrLanguage(PsiClass clazz) {
        if (clazz == null) {
            return false;
        }
        String fqn = clazz.getQualifiedName();
        if ("org.apache.camel.Expression".equals(fqn)
            || "org.apache.camel.Predicate".equals(fqn)
            || "org.apache.camel.model.language.ExpressionDefinition".equals(fqn)
            || "org.apache.camel.builder.ExpressionClause".equals(fqn)) {
            return true;
        }
        // try implements first
        for (PsiClassType ct : clazz.getImplementsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // then fallback as extends
        for (PsiClassType ct : clazz.getExtendsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // okay then go up and try super
        return isCamelExpressionOrLanguage(clazz.getSuperClass());
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    private static String getInnerText(String text) {
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
    private static String getKotlinInnerText(String text) {
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
