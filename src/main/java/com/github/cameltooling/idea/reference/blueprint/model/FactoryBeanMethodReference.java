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
package com.github.cameltooling.idea.reference.blueprint.model;

import com.github.cameltooling.idea.util.BeanUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactoryBeanMethodReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final XmlAttributeValue element;
    private final XmlTag beanTag;

    public FactoryBeanMethodReference(@NotNull XmlAttributeValue element, @NotNull XmlTag beanTag) {
        super(element);
        this.element = element;
        this.beanTag = beanTag;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        return multiResolve(incompleteCode, new HashSet<>());
    }

    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode, Set<String> usedFactoryBeans) {
        BeanUtils beanUtils = BeanUtils.getService();
        List<PsiMethod> methods;
        try {
            methods = beanUtils.findFactoryMethods(beanTag, element.getValue(), usedFactoryBeans);
        } catch (BeanUtils.FactoryBeanMethodCycleException e) {
            return new ResolveResult[] { new FactoryBeanMethodCycleResult() } ;
        }

        if (methods.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY;
        } else {
            return beanUtils.filterPossibleBeanFactoryMethods(beanTag, methods).stream()
                    .map(PsiElementResolveResult::new)
                    .toArray(ResolveResult[]::new);
        }
    }

    public static class FactoryBeanMethodCycleResult implements ResolveResult {

        @Override
        public @Nullable PsiElement getElement() {
            return null;
        }

        @Override
        public boolean isValidResult() {
            return false;
        }

    }

}
