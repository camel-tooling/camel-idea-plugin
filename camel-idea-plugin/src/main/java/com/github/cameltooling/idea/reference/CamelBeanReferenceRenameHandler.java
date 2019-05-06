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
package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.refactoring.rename.PsiElementRenameHandler;

/**
 * Enable renaming of method names in the Camel DSL {@Code "bean(MyClass.class,"myMethod")} and
 * the actually bean method.
 */
public class CamelBeanReferenceRenameHandler extends PsiElementRenameHandler {

    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        final PsiElement psiElement = findPsiElementAt(dataContext);
        if (psiElement == null) {
            return false;
        }
        //Make sure the cursor is located in the text where the method name is defined.
        return psiElement.getParent() instanceof PsiLiteralExpression
            && psiElement.getNextSibling() == null
            && getCamelIdeaUtils().getBean(psiElement) != null;
    }

    private static PsiElement findPsiElementAt(DataContext dataContext) {
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

        if (editor == null) {
            return null;
        }

        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAt == null && editor.getCaretModel().getOffset() > 0) {
            elementAt = file.findElementAt(editor.getCaretModel().getOffset() - 1);
        }
        return elementAt;
    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }
}
