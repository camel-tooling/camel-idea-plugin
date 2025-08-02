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
package com.github.cameltooling.idea.gutter;

import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GutterPsiTargetPresentationRenderer extends PsiTargetPresentationRenderer<PsiElement> {

    @Override
    @NotNull
    public String getElementText(@NotNull PsiElement element) {
        String text = IdeaUtils.getService().extractTextFromElement(element, true, false, true);
        if (text == null) {
            text = element.getText();
        }
        return StringUtil.unquoteString(text);
    }

    @Override
    public @Nls @Nullable String getContainerText(@NotNull PsiElement element) {
        int lineNumber = IdeaUtils.getLineNumber(element);
        return element.getContainingFile().getVirtualFile().getName() + ":" + (lineNumber + 1);
    }

    @Override
    protected Icon getIcon(PsiElement psiElement) {
        return psiElement.getContainingFile().getIcon(Iconable.ICON_FLAG_VISIBILITY);
    }

}
