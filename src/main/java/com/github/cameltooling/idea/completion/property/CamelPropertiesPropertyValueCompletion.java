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
package com.github.cameltooling.idea.completion.property;

import com.github.cameltooling.idea.service.CamelService;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.parsing.PropertiesElementTypes;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link CompletionProvider} that gives the value of the options of main, components, languages and data formats for
 * properties files.
 */
public class CamelPropertiesPropertyValueCompletion extends CamelPropertyValueCompletion {

    @Override
    protected @NotNull PsiElement getCompletionPosition(@NotNull CompletionParameters parameters) {
        final PsiElement element = parameters.getOriginalPosition();
        return element == null ? parameters.getPosition() : element;
    }

    @Override
    protected boolean isEnabled(Project project, PsiFile file) {
        return project.getService(CamelService.class).isCamelProject();
    }

    @Override
    protected @Nullable String getPropertyKey(final PsiElement element) {
        PsiElement previous = element.getPrevSibling();
        while (previous != null) {
            final ASTNode node = previous.getNode();
            if (node != null) {
                final IElementType elementType = node.getElementType();
                if (elementType == PropertiesElementTypes.PROPERTY) {
                    final String result = node.getText();
                    return result.endsWith("=") ? result.substring(0, result.length() - 1) : result;
                } else if (elementType == PropertiesTokenTypes.KEY_CHARACTERS) {
                    return node.getText();
                }
            }
            previous = previous.getPrevSibling();
        }
        return null;
    }
}
