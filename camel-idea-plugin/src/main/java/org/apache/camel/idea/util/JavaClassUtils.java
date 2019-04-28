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
package org.apache.camel.idea.util;

import java.beans.Introspector;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.Disposable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;


public class JavaClassUtils implements Disposable {

    public String getBeanName(PsiClass clazz) {
        final String beanName = getBeanName(clazz, "org.springframework.stereotype.Component")
            .orElseGet(() -> getBeanName(clazz, "org.springframework.stereotype.Service")
                .orElseGet(() -> getBeanName(clazz, "org.springframework.stereotype.Repository")
                    .orElse(Introspector.decapitalize(clazz.getText()))));
        return beanName;
    }

    public Optional<PsiClass> findBeanClassByName(PsiElement element, String annotation) {
        final String elementName = StringUtils.stripDoubleQuotes(element.getText().substring(1, element.getText().indexOf("\"", 2)));
        return getClassesAnnotatedWith(element, annotation).stream()
            .filter(psiClass ->
                Optional.ofNullable(psiClass.getAnnotation(annotation))
                    .map(c ->c.findAttribute("value"))
                    .map(c -> c.getAttributeValue().getSourceElement().getText())
                    .map(beanName -> StringUtils.stripDoubleQuotes(beanName).equals(elementName))
                    .orElseGet(() -> Introspector.decapitalize(psiClass.getName()).equalsIgnoreCase(StringUtils.stripDoubleQuotes(elementName)))

            )
            .findFirst();
    }

    @NotNull
    private Collection<PsiClass> getClassesAnnotatedWith(PsiElement element, String annotation) {
        PsiClass stepClass = JavaPsiFacade.getInstance(element.getProject()).findClass(annotation, ProjectScope.getLibrariesScope(element.getProject()));
        if (stepClass != null) {
            final Query<PsiClass> psiMethods = AnnotatedElementsSearch.searchPsiClasses(stepClass, GlobalSearchScope.allScope(element.getProject()));
            return psiMethods.findAll();
        }
        return Collections.emptyList();
    }

    /**
     * @param referenceElement - Psi Code reference to resolve
     * @return Resolving the Psi reference and return the PsiClass
     */
    public PsiClass resolveClassReference(PsiJavaCodeReferenceElement referenceElement) {
        if (referenceElement != null) {
            PsiReference reference = referenceElement.getReference();

            if (reference != null) {
                final PsiElement resolveElement = reference.resolve();

                if (resolveElement instanceof PsiClass) {
                    return (PsiClass) resolveElement;
                } else if (resolveElement instanceof PsiField) {
                    final PsiType psiType = PsiUtil.getTypeByPsiElement(resolveElement);
                    if (psiType != null) {
                        return ((PsiClassReferenceType) psiType).resolve();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the bean name for the {@link PsiClass} and the specific bean annotation
     * @param clazz - class to return bean name for
     * @param annotationFqn - the lookup FQN string for the annotation
     * @return the bean name
     */
    private Optional<String> getBeanName(PsiClass clazz, String annotationFqn) {
        String returnName = null;
        final PsiAnnotation annotation = clazz.getAnnotation(annotationFqn);
        if (annotation != null) {
            final JvmAnnotationAttribute componentAnnotation = annotation.findAttribute("value");
            returnName = componentAnnotation != null ? StringUtils.stripDoubleQuotes(componentAnnotation.getAttributeValue().getSourceElement().getText()) : Introspector.decapitalize(clazz.getName());
        }
        return Optional.ofNullable(returnName);
    }

    @Override
    public void dispose() {
        //noop
    }
}
