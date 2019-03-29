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
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
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
            CamelIdeaUtils service = CamelIdeaUtils.getService();
            PsiClass beanClass = service.getPropertyBeanClass(tag);
            if (beanClass != null) {
                return IdeaUtils.getService().findSetterMethods(beanClass).stream()
                    .filter(method -> method.getName().toLowerCase().startsWith("set" + query.toLowerCase()))
                    .map(method -> {
                        String methodName = method.getName();
                        String propertyName = getPropertyName(methodName);
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
    private String getPropertyName(String methodName) {
        String methodSuffix = methodName.substring(3);
        if (methodSuffix.length() > 1) {
            if (Character.isUpperCase(methodSuffix.charAt(0)) && Character.isUpperCase(methodSuffix.charAt(1))) {
                return methodSuffix;
            } else {
                return methodSuffix.substring(0, 1).toLowerCase() + methodSuffix.substring(1);
            }
        } else {
            return methodSuffix.toLowerCase();
        }
    }

    @NotNull
    private String getPropertyTypeName(PsiMethod setter) {
        PsiParameter[] parameters = setter.getParameterList().getParameters();
        return parameters[0].getType().getPresentableText();
    }

}
