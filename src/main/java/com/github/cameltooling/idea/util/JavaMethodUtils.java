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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.util.IncorrectOperationException;
import org.apache.camel.component.bean.BeanHelper;
import org.jetbrains.annotations.NotNull;

public class JavaMethodUtils implements Disposable {

    /**
     * Return all method names for the specific class and it's super classes, except
     * Object and Class
     */
    public Collection<PsiMethod> getMethods(PsiClass psiClass) {
        return Stream.of(psiClass.getAllMethods())
            .filter(p -> !p.isConstructor())
            .filter(p -> !Object.class.getName().equals(p.getContainingClass().getQualifiedName()))
            .filter(p -> !Class.class.getName().equals(p.getContainingClass().getQualifiedName()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return all method names for the specific class and it's super classes, except
     * Object and Class
     */
    public Optional<PsiMethod> getHandleMethod(PsiClass psiClass) {
        return Stream.of(psiClass.getAllMethods())
            .filter(p -> p.hasAnnotation("org.apache.camel.Handler"))
            .findFirst();
    }

    /**
     *
     * @param method - method to generate display text for
     * @return a presentable text with parameters for the specific method
     */
    public String getPresentableMethodWithParameters(PsiMethod method) {
        final String parameters = getMethodParameters(method);
        String presentableMethod = method.getName();
        if (!parameters.isEmpty()) {
            presentableMethod = String.format("%s(%s)", method.getName(), parameters);
        }
        return presentableMethod;
    }

    /**
     *
     * @param method - method to generate display text for
     * @return parameters as text separated with comma
     */
    public String getMethodParameters(PsiMethod method) {
        return Arrays.stream(method.getParameters())
            .map(PsiParameter.class::cast)
            .map(PsiParameter::getText)
            .collect(Collectors.joining(", "));
    }

    /**
     *
     * @param method - method to test
     * @param type - to match on.
     * @return true if the method is match one og the jvm modifier
     */
    public boolean isMatchOneOfModifierType(PsiMethod method, String... type) {
        return Stream
            .of(type)
            .anyMatch(m -> method.getModifierList().hasModifierProperty(m));
    }

    /**
     * Return all beans which is not private and abstract;
     * @param methods - List of methods to filter
     * @return - List of filtered methods
     */
    public Collection<PsiMethod> getBeanAccessibleMethods(Collection<PsiMethod> methods) {
        return methods.stream()
            .filter(method -> !isMatchOneOfModifierType(method, PsiModifier.PRIVATE, PsiModifier.ABSTRACT))
            .filter(method -> !method.isConstructor())
            .collect(Collectors.toList());
    }

    /**
     * Filter methods suitable for being a bean method invoked from a route.
     * Private methods are not suitable, but are returned anyway - so that we can show an explicit error message on them
     *
     * @param beanClass - PsiClass representing a bean
     * @param methodSpec - Camel Bean Method specification string, e.g. "myMethod", "myMethod(*, *), "myMethod(java.lang.String, java.lang.Integer)", ...
     * @return - List of methods matching the specification
     */
    public List<PsiMethod> findMatchingBeanMethods(PsiClass beanClass, String methodSpec) {
        String methodName = getMethodNameWithOutParameters(methodSpec);
        PsiMethod[] methods = beanClass.findMethodsByName(methodName, true);
        int paraStart = methodSpec.indexOf('(');
        List<String> paramSpecs;
        if (paraStart >= 0) {
            if (!methodSpec.endsWith(")")) {
                return List.of();
            }
            methodSpec = methodSpec.substring(paraStart + 1, methodSpec.length() - 1);
            paramSpecs = Arrays.stream(methodSpec.split(",")).map(String::trim).toList();
        } else {
            paramSpecs = null;
        }
        return Arrays.stream(methods)
                .filter(method -> !method.isConstructor())
                .filter(method -> !isMethodOverriddenInClass(method, beanClass))
                .filter(method -> paramSpecs == null || beanMethodMatches(method, paramSpecs))
                .toList();
    }

    /**
     * Returns true if the given method is from a super class of the beanClass, and is overridden there.
     * For example, if we've got class A and a class B that extends A, this will return true for methods from A that are overridden in B.
     */
    private boolean isMethodOverriddenInClass(PsiMethod method, PsiClass beanClass) {
        if (beanClass.equals(method.getContainingClass())) {
            return false;
        }
        return beanClass.findMethodBySignature(method, false) != null;
    }

    /**
     * Basically a simplified re-implementation of {@link org.apache.camel.component.bean.BeanInfo#matchMethod}
     */
    private boolean beanMethodMatches(PsiMethod method, List<String> paramSpecs) {
        PsiParameterList methodParams = method.getParameterList();
        int paramCount = methodParams.getParametersCount();
        if (paramCount != paramSpecs.size()) {
            return false;
        }
        for (int i = 0; i < paramSpecs.size(); i++) {
            PsiParameter methodParam = methodParams.getParameter(i);
            if (!parameterMatchesSpec(methodParam, paramSpecs.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean parameterMatchesSpec(PsiParameter param, @NotNull String spec) {
        if (spec.startsWith("${") || spec.equals("*")) {
            return true;
        }
        int typeSeparatorPos = spec.indexOf(' ');
        String typeSpec = spec;
        if (typeSeparatorPos > -1) {
            typeSpec = removeClassSuffix(spec.substring(0, typeSeparatorPos));
        } else {
            if (!typeSpec.endsWith(".class")) {
                typeSpec = determineValueType(spec);
                if (typeSpec == null) {
                    return false;
                }
            } else {
                typeSpec = removeClassSuffix(spec);
            }
        }

        Project project = param.getProject();
        try {
            PsiType type = PsiElementFactory.getInstance(project).createTypeFromText(typeSpec, param);
            return param.getType().isAssignableFrom(type);
        } catch (IncorrectOperationException e) {
            return false;
        }
    }

    private String removeClassSuffix(String value) {
        return value.endsWith(".class") ? value.substring(0, value.length() - ".class".length()) : value;
    }

    private String determineValueType(String value) {
        Class<?> type = BeanHelper.getValidParameterType(value);
        return type == null ? null : type.getName();
    }


    /**
     * Return only the method name in free text from an {@link PsiElement}
     * @param methodLiteral - PsiElement is parse for method name
     * @return the text representation of the method name
     */
    public String getMethodNameWithOutParameters(PsiElement methodLiteral) {
        return getMethodNameWithOutParameters(methodLiteral.getText());
    }

    /**
     * Return only the method name in free text from an {@link String}
     * @param completeMethodWithParmText - The text representation of the method name and it's parameters
     * @return the text representation of the method name
     */
    public String getMethodNameWithOutParameters(String completeMethodWithParmText) {
        if (completeMethodWithParmText.indexOf("(") > 0) {
            return completeMethodWithParmText.substring(0, completeMethodWithParmText.indexOf("("));
        }
        return completeMethodWithParmText;
    }

    @Override
    public void dispose() {
        // noop
    }

    public static JavaMethodUtils getService() {
        return ApplicationManager.getApplication().getService(JavaMethodUtils.class);
    }
}
