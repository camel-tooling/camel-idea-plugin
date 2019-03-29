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

import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Provides references from the value of @BeanInject annotation to the bean definition
 */
public class CamelBeanInjectReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        PsiJavaElementPattern.Capture<PsiLiteralExpression> pattern = PsiJavaPatterns
                .literalExpression()
                .insideAnnotationParam(CamelIdeaUtils.BEAN_INJECT_ANNOTATION);

        registrar.registerReferenceProvider(pattern, new CamelPsiReferenceProvider() {
            @Override
            protected PsiReference[] getCamelReferencesByElement(PsiElement element, ProcessingContext context) {
                PsiNameValuePair param = PsiTreeUtil.getParentOfType(element, PsiNameValuePair.class);
                if (param != null && param.getAttributeName().equals("value")) {
                    String value = param.getLiteralValue();
                    if (value != null) {
                        return new PsiReference[] {new BeanReference(element, value)};
                    }
                }
                return new PsiReference[0];
            }
        });
    }

}