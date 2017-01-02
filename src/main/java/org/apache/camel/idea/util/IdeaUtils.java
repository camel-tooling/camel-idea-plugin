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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

public final class IdeaUtils {

    private IdeaUtils() {
    }

    /**
     * Is the given element a string literal
     */
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
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            PsiMethod method = call.resolveMethod();
            if (method != null) {
                String name = method.getName();
                return "from".equals(name) || "fromF".equals(name) || "interceptFrom".equals(name) || "pollEnrich".equals(name);
            }
        }
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Consume");
        }

        return false;
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    public static String getInnerText(PsiJavaToken token) {
        String text = token.getText();
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
}
