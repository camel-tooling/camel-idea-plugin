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
package com.github.cameltooling.idea.annotator;

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.JavaClassUtils;
import com.github.cameltooling.idea.util.JavaMethodUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Annotate camel bean reference with error if the method is private or does not exist
 */
public class CamelBeanMethodAnnotator implements Annotator {

    private static final String METHOD_CAN_NOT_RESOLVED = "Can not resolve method '%s' in bean '%s'";
    private static final String METHOD_HAS_PRIVATE_ACCESS = "'%s' has private access in bean '%s'";
    private static final String METHOD_HAS_AMBIGUOUS_ACCESS = "Ambiguous matches '%s' in bean '%s'";
    private static final Logger LOG = Logger.getInstance(CamelBeanMethodAnnotator.class);

    boolean isEnabled(@NotNull PsiElement element) {
        final IdeaUtils ideaUtils = IdeaUtils.getService();
        final boolean valid = element.getProject().getService(CamelService.class).isCamelPresent()
            && CamelPreferenceService.getService().isRealTimeSimpleValidation()
            // skip whitespace noise
            && !ideaUtils.isWhiteSpace(element)
            // skip java doc noise
            && !ideaUtils.isJavaDoc(element)
            && CamelIdeaUtils.getService().getBeanPsiElement(element) != null;
        return valid;
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!isEnabled(element)) {
            return;
        }

        final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();
        PsiClass psiClass = camelIdeaUtils.getBean(element);

        if (psiClass == null) {
            return;
        }

        String errorMessage;
        final JavaMethodUtils javaMethodUtils = JavaMethodUtils.getService();
        final String beanName = JavaClassUtils.getService().getBeanName(psiClass);
        final String methodNameWithParameters = StringUtils.stripDoubleQuotes(element.getText());
        final String methodName = StringUtils.stripDoubleQuotes(javaMethodUtils.getMethodNameWithOutParameters(element));

        if (methodName.equals(beanName)) {
            //We don't want to check psiClass elements itself
            return;
        }

        final List<PsiMethod> matchMethods = getMatchingMethods(psiClass, methodName);

        if (matchMethods.isEmpty()) {
            errorMessage = matchMethods.isEmpty() ? String.format(METHOD_CAN_NOT_RESOLVED, methodNameWithParameters, psiClass.getQualifiedName()) : null;
        } else {
            final long privateMethods = matchMethods.stream()
                .filter(method -> javaMethodUtils.isMatchOneOfModifierType(method, PsiModifier.PRIVATE))
                .count();

            final boolean isAnnotatedWithHandler = matchMethods.stream().anyMatch(psiMethod -> camelIdeaUtils.isAnnotatedWithHandler(psiMethod));

            final boolean allPrivates = privateMethods == matchMethods.size();

            if (methodNameWithParameters.indexOf("(", methodName.length()) > 0 && methodNameWithParameters.endsWith(")") && !allPrivates) {
                //TODO implement logic for matching on parameters.
                return;
            }

            if ((matchMethods.size() - privateMethods) > 1 && (!isAnnotatedWithHandler)) {
                errorMessage = String.format(METHOD_HAS_AMBIGUOUS_ACCESS, methodNameWithParameters, psiClass.getQualifiedName());
            } else {
                errorMessage = allPrivates ? String.format(METHOD_HAS_PRIVATE_ACCESS, methodNameWithParameters, psiClass.getQualifiedName()) : null;
            }
        }

        if (errorMessage != null) {
            holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
                    .range(element).create();
        }
    }

    @NotNull
    public List<PsiMethod> getMatchingMethods(PsiClass psiClass, String methodName) {
        final JavaMethodUtils javaMethodUtils = JavaMethodUtils.getService();
        return javaMethodUtils.getBeanMethods(javaMethodUtils.getMethods(psiClass))
                .stream()
                .peek(method -> LOG.debug("element %s = %s method in bean %s", methodName, StringUtils.stripDoubleQuotes(method.getName()), psiClass.getQualifiedName()))
                .filter(method -> StringUtils.stripDoubleQuotes(method.getName()).equals(methodName))
                .collect(toList());
    }
}
