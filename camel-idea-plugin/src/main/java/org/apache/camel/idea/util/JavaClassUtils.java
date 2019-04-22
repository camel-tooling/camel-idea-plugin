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
        final PsiAnnotation annotation = clazz.getAnnotation("org.springframework.stereotype.Component");
        if (annotation != null) {
            final JvmAnnotationAttribute componentAnnotation = annotation.findAttribute("value");
            return componentAnnotation != null ? StringUtils.stripDoubleQuotes(componentAnnotation.getAttributeValue().getSourceElement().getText()) :  Introspector.decapitalize(clazz.getText());
        }
        return Introspector.decapitalize(clazz.getText());
    }

    public Optional<PsiClass> findBeanClassByName(PsiElement element, String annotation) {
        final String elementName = StringUtils.stripDoubleQuotes(element.getText().substring(1, element.getText().indexOf("\"", 2)));
        return getClassesAnnotatedWith(element, annotation).stream()
            .filter(psiClass ->
                Optional.ofNullable(psiClass.getAnnotation(annotation))
                    .map(c ->c.findAttribute("value"))
                    .map(c -> c.getAttributeValue().getSourceElement().getText())
                    .map(beanName -> StringUtils.stripDoubleQuotes(beanName).equals(elementName))
                    .orElseGet(() -> Introspector.decapitalize(psiClass.getName()).equalsIgnoreCase(StringUtils.stripDoubleQuotes(element.getText())))

            ).findFirst();
    }

    @NotNull
    public Collection<PsiClass> getClassesAnnotatedWith(PsiElement element, String annotation) {
        //search for component, service annotations
        PsiClass stepClass = JavaPsiFacade.getInstance(element.getProject()).findClass(annotation, ProjectScope.getLibrariesScope(element.getProject()));
        final Query<PsiClass> psiMethods = AnnotatedElementsSearch.searchPsiClasses(stepClass, GlobalSearchScope.allScope(element.getProject()));
        return psiMethods.findAll();
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

    @Override
    public void dispose() {
        //noop
    }
}
