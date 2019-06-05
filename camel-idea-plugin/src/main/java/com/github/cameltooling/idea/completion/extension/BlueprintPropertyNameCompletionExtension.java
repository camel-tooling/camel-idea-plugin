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
package com.github.cameltooling.idea.completion.extension;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.github.cameltooling.idea.reference.blueprint.PropertyNameReference;
import com.github.cameltooling.idea.util.BeanUtils;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Code completion for property names in blueprint xml files, based on {@link PropertyNameReference}
 */
public class BlueprintPropertyNameCompletionExtension extends ReferenceBasedCompletionExtension<PropertyNameReference> {

    public BlueprintPropertyNameCompletionExtension() {
        super(PropertyNameReference.class);
    }

    @Override
    protected List<LookupElement> findResults(@NotNull PsiElement element,
                                              @NotNull String query) {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (tag != null) {
            PsiClass beanClass = BeanUtils.getService().getPropertyBeanClass(tag);
            if (beanClass != null) {
                return PropertyUtilBase.getAllProperties(beanClass, true, false).entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(query.toLowerCase()))
                    .map(entry -> {
                        String propertyName = entry.getKey();
                        PsiMethod method = entry.getValue();
                        return createLookupElementBuilder(propertyName, method)
                            .withTypeText(getPropertyTypeName(method))
                            .withTailText("")
                            .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE);
                    })
                    .sorted((a, b) -> {
                        String first = a.getLookupString();
                        String second = b.getLookupString();
                        return first.compareToIgnoreCase(second);
                    })
                    .collect(Collectors.toList());

            }
        }
        return Collections.emptyList();
    }

    @NotNull
    private String getPropertyTypeName(PsiMethod setter) {
        PsiParameter[] parameters = setter.getParameterList().getParameters();
        return parameters[0].getType().getPresentableText();
    }

}
