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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.reference.blueprint.model.ReferenceableBeanId;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.util.BeanUtils;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * Code completion for bean references, based on {@link BeanReference}
 */
public class BeanReferenceCompletionExtension extends ReferenceBasedCompletionExtension<BeanReference> {

    public BeanReferenceCompletionExtension() {
        super(BeanReference.class);
    }

    @Override
    protected List<LookupElement> findResults(@NotNull PsiElement element, @NotNull String query) {
        List<ReferenceableBeanId> targets = findTargets(element, query);
        return targets.stream()
                .map(bean -> createLookupElementBuilder(bean.getId(), bean.getElement())
                .withTypeText(bean.getReferencedClass() == null ? null : bean.getReferencedClass().getClassSimpleName())
                .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE))
                .collect(Collectors.toList());
    }

    private List<ReferenceableBeanId> findTargets(PsiElement element, String query) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return Collections.emptyList();
        }

        PsiType expectedType = getExpectedType(element);

        Predicate<String> beanIdPredicate = b -> b.startsWith(query);
        if (expectedType != null) {
            return BeanUtils.getService().findReferenceableBeanIdsByType(module, beanIdPredicate, expectedType);
        } else {
            return BeanUtils.getService().findReferenceableBeanIds(module, beanIdPredicate);
        }
    }

    private PsiType getExpectedType(PsiElement element) {
        if (!CamelPreferenceService.getService().isRealTimeIdReferenceTypeValidation()) {
            return null;
        }
        return BeanUtils.getService().findExpectedBeanTypeAt(element);
    }

}
