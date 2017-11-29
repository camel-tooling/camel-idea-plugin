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

import java.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.idea.extension.IdeaUtilsExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

public class JavaIdeaUtils implements IdeaUtilsExtension {
    @Override
    public Optional<String> extractTextFromElement(PsiElement element, boolean concatString, boolean stripWhitespace) {
        if (element instanceof PsiLiteralExpression) {
            // need the entire line so find the literal expression that would hold the entire string (java)
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            Object o = literal.getValue();
            String text = o != null ? o.toString() : null;
            if (text == null) {
                return Optional.empty();
            }
            if (concatString) {
                final PsiPolyadicExpression parentOfType = PsiTreeUtil.getParentOfType(element, PsiPolyadicExpression.class);
                if (parentOfType != null) {
                    text = parentOfType.getText();
                }
            }
            // unwrap literal string which can happen in java too
            if (stripWhitespace) {
                return Optional.ofNullable(getInnerText(text));
            }
            return Optional.of(StringUtil.unquoteString(text.replace(QUOT, "\"")));
        }
        return Optional.empty();
    }

    @Override
    public boolean isElementFromSetterProperty(@NotNull PsiElement element, @NotNull String setter) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            PsiMethod resolved = call.resolveMethod();
            if (resolved != null) {
                String javaSetter = "set" + Character.toUpperCase(setter.charAt(0)) + setter.substring(1);
                return javaSetter.equals(resolved.getName());
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
}
