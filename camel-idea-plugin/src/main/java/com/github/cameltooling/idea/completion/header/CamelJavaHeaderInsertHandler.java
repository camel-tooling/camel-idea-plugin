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
package com.github.cameltooling.idea.completion.header;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

/**
 * {@code CamelJavaHeaderInsertHandler} allows to use the simple class name in a suggestion by automatically importing
 * if possible otherwise it automatically prefixes the name of the class with the name of its package in the injected
 * suggestion.
 */
class CamelJavaHeaderInsertHandler implements InsertHandler<LookupElement> {

    /**
     * The name of the class to try to automatically import.
     */
    private final String className;
    /**
     * The expression using the class to import.
     */
    private final String expression;
    /**
     * The length of the prefix to consider when injecting the name of its package in the injected suggestion.
     */
    private final int prefixLength;

    /**
     * Constructs a {@code CamelJavaHeaderInsertHandler} with the given parameters.
     * @param className the name of the class to try to automatically import.
     * @param expression the expression using the class to import.
     * @param prefixLength the length of the prefix to consider when injecting the name of its package in the injected
     *                     suggestion.
     */
    CamelJavaHeaderInsertHandler(String className, String expression, int prefixLength) {
        this.className = className;
        this.expression = expression;
        this.prefixLength = prefixLength;
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        final Project project = context.getProject();
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        final PsiClass resolved = JavaPsiFacade.getInstance(project)
            .getResolveHelper().resolveReferencedClass(
                className, factory.createExpressionFromText(expression, null)
            );
        if (resolved == null) {
            // Use FQN of the class in case the class is not found
            injectPackageName(className, context, item);
        } else if (context.getFile() instanceof PsiJavaFile) {
            final PsiJavaFile javaFile = (PsiJavaFile) context.getFile();
            // Auto import the class if available
            if (!javaFile.importClass(resolved) && javaFile.findImportReferenceTo(resolved) == null) {
                // Use FQN of the class in case there is a name conflict
                injectPackageName(className, context, item);
            }
        }
    }

    /**
     * Injects the name of the package to the suggestion.
     */
    private void injectPackageName(String className, InsertionContext context, LookupElement item) {
        context.getDocument().insertString(
            context.getEditor().getCaretModel().getOffset() - item.getLookupString().length() + prefixLength,
            className.substring(0, className.lastIndexOf('.') + 1)
        );
    }
}
