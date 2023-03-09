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

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.JavaClassUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import static com.intellij.patterns.PsiJavaPatterns.psiLiteral;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;
import static com.intellij.patterns.StandardPatterns.or;

/**
 * Create a link between the Camel DSL {@code bean(MyClass.class,"myMethod")} and the specific method
 * in it's destination bean.
 */
public class CamelBeanReferenceContributor extends PsiReferenceContributor {

    public static final PsiJavaElementPattern.Capture<PsiLiteral> BEAN_CLASS_METHOD_PATTERN = psiLiteral().methodCallParameter(
        psiMethod().withName("bean").withParameters("java.lang.Class", "java.lang.String")
    );

    public static final PsiJavaElementPattern.Capture<PsiLiteral> METHOD_CLASS_METHOD_PATTERN = psiLiteral().methodCallParameter(
        psiMethod().withName("method").withParameters("java.lang.Class", "java.lang.String")
    );

    public static final PsiJavaElementPattern.Capture<PsiLiteral> BEAN_OBJECT_STRING_PATTERN = psiLiteral().methodCallParameter(
        psiMethod().withName("bean").withParameters("java.lang.Object", "java.lang.String")
    );

    public static final PsiJavaElementPattern.Capture<PsiLiteral> METHOD_STRING_STRING_PATTERN = psiLiteral().methodCallParameter(
        psiMethod().withName("method").withParameters("java.lang.String", "java.lang.String")
    );

    public static final PsiJavaElementPattern.Capture<PsiLiteral> METHOD_OBJECT_STRING_PATTERN = psiLiteral().methodCallParameter(
        psiMethod().withName("method").withParameters("java.lang.Object", "java.lang.String")
    );

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(or(BEAN_CLASS_METHOD_PATTERN, METHOD_CLASS_METHOD_PATTERN), new CamelPsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getCamelReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                return createCamelBeanMethodReference(element);
            }
        });

        registrar.registerReferenceProvider(or(BEAN_OBJECT_STRING_PATTERN, METHOD_STRING_STRING_PATTERN, METHOD_OBJECT_STRING_PATTERN), new CamelPsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getCamelReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                return createSpringBeanMethodReference(element);
            }
        });
    }

    private PsiReference[] createCamelBeanMethodReference(@NotNull PsiElement element) {

        if (element.getText().contains("IntellijIdeaRulezzz")) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiClass psiClass = CamelIdeaUtils.getService().getBean(element);
        if (psiClass != null) {
            String methodName = StringUtils.stripDoubleQuotes(element.getText());
            if (!methodName.isEmpty()) {
                return new PsiReference[] {new CamelBeanMethodReference(element, psiClass, methodName, new TextRange(1, methodName.length() + 1))};
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] createSpringBeanMethodReference(@NotNull PsiElement element) {

        if (element.getText().contains("IntellijIdeaRulezzz")) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiClass psiClass = CamelIdeaUtils.getService().getBean(element);
        if (psiClass == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        final String beanName = JavaClassUtils.getService().getBeanName(psiClass);
        final String methodName = StringUtils.stripDoubleQuotes(element.getText());

        if (methodName.equals(beanName) || methodName.isEmpty()) {
            return PsiReference.EMPTY_ARRAY;
        }

        return new PsiReference[] {new CamelBeanMethodReference(element, psiClass, methodName, new TextRange(1, methodName.length() + 1))};

    }

}
