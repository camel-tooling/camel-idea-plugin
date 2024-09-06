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
package com.github.cameltooling.idea.util;

import java.beans.Introspector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class JavaClassUtils implements Disposable {

    /**
     * The prefix of all the classes in the java lang package.
     */
    private static final String JAVA_LANG_PACKAGE = "java.lang.";
    private static final List<String> BEAN_ANNOTATIONS = Arrays.asList(
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Repository",
        "javax.inject.Named",
        "jakarta.inject.Named"
    );

    public static JavaClassUtils getService() {
        return ApplicationManager.getApplication().getService(JavaClassUtils.class);
    }

    /**
     * @return the bean name of the {@link PsiClass} by check if the class is annotated with Component, Service, Repository or defaulting
     * to class name decapitalized
     */
    public String getBeanName(PsiClass clazz) {
        return BEAN_ANNOTATIONS
                .stream()
                .map(annotation -> getBeanName(clazz, annotation))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> Introspector.decapitalize(clazz.getText()));
    }

    /**
     * Searching for the specific bean name and annotation to find it's {@link PsiClass}
     * @param beanName - Name of the bean to search for.
     * @param annotation - Type of bean annotation to filter on.
     * @param project - Project reference to narrow the search inside.
     * @return the {@link PsiClass} matching the bean name and annotation.
     */
    public Optional<PsiClass> findBeanClassByName(String beanName, String annotation, Project project) {
        for (PsiClass psiClass : getClassesAnnotatedWith(project, annotation)) {
            final PsiAnnotation classAnnotation = psiClass.getAnnotation(annotation);
            PsiAnnotationMemberValue attribute = classAnnotation.findAttributeValue("value");
            if (attribute != null) {
                if (attribute instanceof PsiReferenceExpressionImpl) {
                    //if the attribute value is field reference eg @bean(value = MyClass.BEAN_NAME)
                    final PsiField psiField = (PsiField) attribute.getReference().resolve();
                    String staticBeanName = StringUtils.stripDoubleQuotes(PsiTreeUtil.getChildOfAnyType(psiField, PsiLiteralExpression.class).getText());
                    if (beanName.equals(staticBeanName)) {
                        return Optional.of(psiClass);
                    }
                } else {
                    final String value = attribute.getText();
                    if (beanName.equals(StringUtils.stripDoubleQuotes(value))) {
                        return Optional.of(psiClass);
                    }
                }
            } else {
                if (StringUtils.stripDoubleQuotes(beanName).equalsIgnoreCase(Introspector.decapitalize(psiClass.getName()))) {
                    return Optional.of(psiClass);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Return a list of {@link PsiClass}s annotated with the specified annotation
     * @param project - Project reference to narrow the search inside.
     * @param annotation - the full qualify annotation name to search for
     * @return a list of classes annotated with the specified annotation.
     */
    @NotNull
    private Collection<PsiClass> getClassesAnnotatedWith(Project project, String annotation) {
        PsiClass stepClass = JavaPsiFacade.getInstance(project).findClass(annotation, ProjectScope.getLibrariesScope(project));
        if (stepClass != null) {
            final Query<PsiClass> psiMethods = AnnotatedElementsSearch.searchPsiClasses(stepClass, GlobalSearchScope.allScope(project));
            return psiMethods.findAll();
        }
        return Collections.emptyList();
    }

    public JavaClassReference findClassReference(@NotNull PsiElement element) {
        List<JavaClassReference> references = Arrays.stream(element.getReferences())
                .filter(JavaClassReference.class::isInstance)
                .map(JavaClassReference.class::cast)
                .toList();
        if (!references.isEmpty()) {
            return references.get(references.size() - 1);
        }
        return null;
    }

    public PsiClass resolveClassReference(@NotNull PsiReference reference) {
        final PsiElement resolveElement = reference.resolve();

        if (resolveElement instanceof PsiClass psiClass) {
            return psiClass;
        } else if (resolveElement instanceof PsiField) {
            final PsiType psiType = PsiUtil.getTypeByPsiElement(resolveElement);
            if (psiType != null) {
                return ((PsiClassReferenceType) psiType).resolve();
            }
        }

        return null;
    }

    /**
     * @param referenceElement - Psi Code reference element to resolve
     * @return Resolving the Psi reference and return the PsiClass
     */
    public PsiClass resolveClassReference(@Nullable PsiJavaCodeReferenceElement referenceElement) {
        if (referenceElement != null) {
            PsiReference reference = referenceElement.getReference();

            if (reference != null) {
                return resolveClassReference(reference);
            }
        }
        return null;
    }

    /**
     * @param type the Java type to simplify if needed.
     * @return the primitive type in case of wrapper classes. {@code string} in case of {@link String}. The given type
     * otherwise.
     */
    @Nullable
    public String toSimpleType(@Nullable String type) {
        if (type == null) {
            return null;
        }
        String result = type.toLowerCase();
        if (result.startsWith(JAVA_LANG_PACKAGE)) {
            result = result.substring(JAVA_LANG_PACKAGE.length());
        }
        return switch (result) {
        case "string", "long", "boolean", "double", "float", "short", "char", "byte", "int" -> result;
        case "character" -> "char";
        case "integer" -> "int";
        default -> type;
        };
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
            final PsiAnnotationMemberValue componentAnnotation = annotation.findAttributeValue("value");
            returnName = componentAnnotation != null ? StringUtils.stripDoubleQuotes(componentAnnotation.getText()) : Introspector.decapitalize(clazz.getName());
        }
        return Optional.ofNullable(returnName);
    }

    @Override
    public void dispose() {
        //noop
    }
}
