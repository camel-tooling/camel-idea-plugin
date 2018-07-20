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
package org.apache.camel.idea.reference;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Create a link between the Camel DSL {@Code bean(MyClass.class,"myMethod")} and the specific method
 * in it's destination bean.
 */
public class CamelBeanReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        final PsiElementPattern.Capture<PsiLiteralExpression> pattern = PlatformPatterns.psiElement(PsiLiteralExpression.class)
            .withParent(PsiExpressionList.class);
        registrar.registerReferenceProvider(pattern, new PsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                if (!ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
                    return PsiReference.EMPTY_ARRAY;
                }
                return createCamelBeanMethodReference(element);
            }
        });
    }

    private PsiReference[] createCamelBeanMethodReference(@NotNull PsiElement element) {

        final PsiElement beanClassElement = getCamelIdeaUtils().getBeanPsiElement(element);
        if (beanClassElement != null) {
            PsiClass psiClass = getCamelIdeaUtils().getBean(element);
            if (psiClass != null) {
                final PsiLiteral beanNameElement = PsiTreeUtil.findChildOfType(PsiTreeUtil.getParentOfType(beanClassElement, PsiExpressionList.class), PsiLiteral.class);
                String methodName = beanNameElement.getText().replace("\"", "");
                if (!methodName.isEmpty()) {
                    return new PsiReference[] {new CamelBeanMethodReference(element, psiClass, methodName, new TextRange(1, methodName.length() + 1))};
                }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

}
