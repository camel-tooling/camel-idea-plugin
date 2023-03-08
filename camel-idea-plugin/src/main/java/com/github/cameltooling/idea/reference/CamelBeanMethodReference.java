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
package com.github.cameltooling.idea.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.JavaMethodUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;


/**
 * A reference between the Camel DSL bean ref method ".bean(MyClass.class,"myMethod") and the {@link PsiMethod}
 */
public class CamelBeanMethodReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final PsiClass psiClass;
    private final String methodName;
    private final String methodNameOnly;

    /**
     * Reference between the Camel bean method DSL and the actually method.
     * @param element - The Camel bean method element
     * @param psiClass - The Class the method referer to exist in
     * @param methodName - The name of the method it referer from
     * @param textRange - the text range
     */
    CamelBeanMethodReference(PsiElement element, PsiClass psiClass, String methodName, TextRange textRange) {
        super(element, textRange);
        this.psiClass = psiClass;
        this.methodName = methodName;
        this.methodNameOnly = getJavaMethodUtils().getMethodNameWithOutParameters(methodName);
    }

    @NotNull
    @Override
    public ResolveResult @NotNull [] multiResolve(boolean b) {
        List<ResolveResult> results = new ArrayList<>();

        final PsiMethod[] methodsByName = getPsiClass().findMethodsByName(methodNameOnly, true);
        for (PsiMethod psiMethod : getJavaMethodUtils().getBeanMethods(Arrays.asList(methodsByName))) {
            if (getCamelIdeaUtils().isAnnotatedWithHandler(psiMethod)) {
                return new ResolveResult[] {new PsiElementResolveResult(psiMethod)};
            }
            results.add(new PsiElementResolveResult(psiMethod));
        }
        return results.toArray(new ResolveResult[0]);
    }

    @NotNull
    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0];
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        //Find all the method with the registered method name on it's class.

        final PsiMethod[] methodsByName = getPsiClass().findMethodsByName(methodNameOnly, true);

        for (PsiMethod psiMethod : methodsByName) {
            psiMethod.setName(newElementName);
        }
        //Rename the Camel DSL bean ref method
        ElementManipulators.getManipulator(getElement()).handleContentChange(getElement(), this.getRangeInElement(), newElementName);
        return getElement();
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    private PsiClass getPsiClass() {
        return psiClass;
    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return CamelIdeaUtils.getService();
    }

    private JavaMethodUtils getJavaMethodUtils() {
        return ApplicationManager.getApplication().getService(JavaMethodUtils.class);
    }
}
