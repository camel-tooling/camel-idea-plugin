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
package org.apache.camel.idea.refereance;

import java.util.ArrayList;
import java.util.List;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A reference between the Camel DSL bean ref method ".bean(MyClass.class,"myMethod") and the {@link PsiMethod}
 *
 */
public class CamelBeanMethodReference extends PsiReferenceBase<PsiClass> implements PsiPolyVariantReference {

    private final PsiLiteral beanNameElement;
    private final String methodName;

    CamelBeanMethodReference(PsiClass element, PsiLiteral beanNameElement, String methodName, TextRange textRange) {
        super(element, textRange);
        this.beanNameElement = beanNameElement;
        this.methodName = methodName;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {
        List<ResolveResult> results = new ArrayList<>();
        final PsiMethod[] methodsByName = getElement().findMethodsByName(methodName, true);
        for (PsiMethod psiMethod : methodsByName) {
            final boolean isPrivate = getCamelIdeaUtils().isOneOfModifierType(psiMethod, JvmModifier.PRIVATE, JvmModifier.ABSTRACT);

            if (!isPrivate) {
                if (getCamelIdeaUtils().isAnnotatedWithHandler(psiMethod)) {
                    return new ResolveResult[] {new PsiElementResolveResult(psiMethod)};
                }
                results.add(new PsiElementResolveResult(psiMethod));
            }
        }
        return results.toArray(new ResolveResult[results.size()]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        //Find all the method with the registered method name on it's class.
        final PsiMethod[] methodsByName = getElement().findMethodsByName(methodName, true);

        for (PsiMethod psiMethod : methodsByName) {
            psiMethod.setName(newElementName);
        }
        //Rename the Camel DSL bean ref method
        ElementManipulators.getManipulator(beanNameElement).handleContentChange(this.beanNameElement, this.getRangeInElement(), newElementName);
        return getElement();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return super.bindToElement(element);
    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

    @Override
    public boolean isSoft() {
        return false;
    }
}
