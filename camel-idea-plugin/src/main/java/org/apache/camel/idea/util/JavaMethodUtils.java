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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.Disposable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

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
    public boolean isMatchOneOfModifierType(PsiMethod method, JvmModifier... type) {
        return Arrays.stream(method.getModifiers())
            .anyMatch(m -> Arrays.stream(type).anyMatch(Predicate.isEqual(m)));
    }

    /**
     * Return all beans which is not private and abstract;
     * @param methods - List of methods to filter
     * @return - List of filtered methods
     */
    public Collection<PsiMethod> getBeanAccessibleMethods(Collection<PsiMethod> methods) {
        return methods.stream()
            .filter(method -> !isMatchOneOfModifierType(method, JvmModifier.PRIVATE, JvmModifier.ABSTRACT))
            .filter(method -> !method.isConstructor())
            .collect(Collectors.toList());
    }

    /**
     * Return all methods expect the constructor
     * @param methods - List of methods to filter
     * @return - List of filtered methods
     */
    public Collection<PsiMethod> getBeanMethods(Collection<PsiMethod> methods) {
        return methods.stream()
            .filter(method -> !method.isConstructor())
            .collect(Collectors.toList());
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
}
