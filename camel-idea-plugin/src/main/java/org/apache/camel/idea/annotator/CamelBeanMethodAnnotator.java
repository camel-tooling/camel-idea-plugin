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
package org.apache.camel.idea.annotator;

import java.util.List;
import static java.util.stream.Collectors.toList;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.apache.camel.idea.util.IdeaUtils;
import org.apache.camel.idea.util.JavaMethodUtils;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Annotate camel bean reference with error if the method is private or does not exist
 */
public class CamelBeanMethodAnnotator implements Annotator {

    public static final String METHOD_CAN_NOT_RESOLVED = "Can not resolve method '%s' in bean '%s'";
    public static final String METHOD_HAS_PRIVATE_ACCESS = "'%s' has private access in bean '%s'";
    private static final Logger LOG = Logger.getInstance(CamelBeanMethodAnnotator.class);

    boolean isEnabled() {
        return ServiceManager.getService(CamelPreferenceService.class).isRealTimeSimpleValidation();
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!isEnabled()) {
            return;
        }

        // skip whitespace noise
        if (getIdeaUtils().isWhiteSpace(element)) {
            return;
        }

        // skip java doc noise
        if (getIdeaUtils().isJavaDoc(element)) {
            return;
        }

        final PsiElement beanClassElement = getCamelIdeaUtils().getBeanPsiElement(element);
        if (beanClassElement != null) {
            PsiClass psiClass = getCamelIdeaUtils().getBean(element);
            if (psiClass != null) {
                String errorMessage;
                final String methodName = StringUtils.stripDoubleQuotes(element.getText());
                final List<PsiMethod> matchMethods = getJavaMethodUtils().getBeanMethods(getJavaMethodUtils().getMethods(psiClass))
                    .stream()
                    .peek(method -> {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(String.format("element %s = %s method in bean %s", methodName, StringUtils.stripDoubleQuotes(method.getName()), psiClass.getQualifiedName()));
                        }
                    })
                    .filter(method -> StringUtils.stripDoubleQuotes(method.getName()).equals(methodName))
                    .collect(toList());

                if (matchMethods.isEmpty()) {
                    errorMessage = matchMethods.isEmpty() ? String.format(METHOD_CAN_NOT_RESOLVED, methodName, psiClass.getQualifiedName()) : null;
                } else {
                    errorMessage = matchMethods.stream()
                        .allMatch(method -> getJavaMethodUtils().isMatchOneOfModifierType(method, JvmModifier.PRIVATE))
                        ? String.format(METHOD_HAS_PRIVATE_ACCESS, methodName, psiClass.getQualifiedName()) : null;
                }

                if (errorMessage != null) {
                    holder.createErrorAnnotation(element, errorMessage);
                }
            }
        }

    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

    private JavaMethodUtils getJavaMethodUtils() {
        return ServiceManager.getService(JavaMethodUtils.class);
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }
}
