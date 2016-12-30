/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import javax.swing.*;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

public class CamelAddEndpointIntention extends PsiElementBaseIntentionAction {

    // TODO: Add Camel icon

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        // TODO: fetch which Camel JARs are on classpath and filter the component names accordingly from the camel catalog
        Object[] data = new Object[]{"log", "seda", "timer"};
        JList list = new JList(data);

        PopupChooserBuilder builder = JBPopupFactory.getInstance().createListPopupBuilder(list);
        builder.setAdText("Choose Camel Component");
        builder.setItemChoosenCallback(() -> {
            String line = (String) list.getSelectedValue();
            int pos = editor.getCaretModel().getCurrentCaret().getOffset();
            if (pos > 0) {
                // must run this as write action because we change the source code
                new WriteCommandAction(project, element.getContainingFile()) {
                    @Override
                    protected void run(@NotNull Result result) throws Throwable {
                        String text = line + ":";
                        editor.getDocument().insertString(pos, text);
                        editor.getCaretModel().moveToOffset(pos + text.length());
                    }
                }.execute();
            }
        });

        JBPopup popup = builder.createPopup();
        popup.showInBestPositionFor(editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // if its a string literal
        if (IdeaUtils.isStringLiteral(element)) {
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            String text = (String) literal.getValue();
            // only be available if the string is empty
            return text == null || text.isEmpty();
        }
        if (IdeaUtils.isJavaTokenLiteral(element)) {
            PsiJavaToken token = (PsiJavaToken) element;
            String text = getInnerText(token);
            return text == null || text.isEmpty();
        }
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return "Add camel endpoint";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Apache Camel";
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    private String getInnerText(PsiJavaToken token) {
        String text = token.getText();
        int textLength = text.length();
        if (StringUtil.endsWithChar(text, '\"')) {
            if (textLength == 1) return null;
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
