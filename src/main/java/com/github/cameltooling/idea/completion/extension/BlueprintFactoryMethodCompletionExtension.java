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
package com.github.cameltooling.idea.completion.extension;

import com.github.cameltooling.idea.completion.lookup.MethoLookupElementFactory;
import com.github.cameltooling.idea.reference.blueprint.model.FactoryBeanMethodReference;
import com.github.cameltooling.idea.util.BeanUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlueprintFactoryMethodCompletionExtension extends ReferenceBasedCompletionExtension<FactoryBeanMethodReference> {

    public BlueprintFactoryMethodCompletionExtension() {
        super(FactoryBeanMethodReference.class);
    }

    @Override
    public boolean supportsSmartCompletion() {
        return true;
    }

    @Override
    protected List<LookupElement> findResults(@NotNull CompletionParameters parameters, @NotNull PsiElement element, @NotNull String query) {
        XmlAttribute xAttr = PsiTreeUtil.getParentOfType(element, XmlAttribute.class, false);
        if (xAttr != null && xAttr.getLocalName().equals("factory-method")) {
            XmlTag tag = xAttr.getParent();
            BeanUtils beanUtils = BeanUtils.getService();
            if (beanUtils.isBeanDeclaration(tag)) {
                try {
                    List<PsiMethod> methods = beanUtils.findFactoryMethods(tag, null);
                    if (parameters.getCompletionType().equals(CompletionType.SMART)) {
                        methods = beanUtils.filterPossibleBeanFactoryMethods(tag, methods);
                    }
                    return convertMethodsToLookupElements(methods);
                } catch (BeanUtils.FactoryBeanMethodCycleException e) {
                    return List.of();
                }
            }
        }
        return List.of();
    }

    private List<LookupElement> convertMethodsToLookupElements(List<PsiMethod> methods) {
        Set<String> names = new HashSet<>();
        return methods.stream()
                .filter(m -> {
                    if (names.contains(m.getName())) {
                        return false;
                    }
                    names.add(m.getName());
                    return true;
                })
                .map(method -> {
                    PsiType rt = method.getReturnType();
                    return MethoLookupElementFactory.create(method, method.getName(), rt == null ? null : rt.getPresentableText());
                })
                .toList();
    }

}
