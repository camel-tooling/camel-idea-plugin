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
package org.apache.camel.idea.gutter;

import javax.swing.*;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.Nullable;

/**
 * A gutter navigation list renderer to customize the display of the related Camel routes.
 */
class GutterPsiElementListCellRenderer extends PsiElementListCellRenderer {
    @Override
    public String getElementText(PsiElement element) {
        if (element instanceof PsiLiteralExpression) {
            return ((PsiLiteralExpression) element).getValue().toString();
        }
        return element.getText();
    }

    @Nullable
    @Override
    protected String getContainerText(PsiElement element, String s) {
        return element.getContainingFile().getVirtualFile().getName();
    }

    @Override
    protected int getIconFlags() {
        return Iconable.ICON_FLAG_VISIBILITY;
    }

    @Override
    protected Icon getIcon(PsiElement psiElement) {
        return psiElement.getContainingFile().getIcon(Iconable.ICON_FLAG_VISIBILITY);
    }
}
