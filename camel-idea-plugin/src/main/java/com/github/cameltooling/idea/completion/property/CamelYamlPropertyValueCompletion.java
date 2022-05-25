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

import java.util.Deque;
import java.util.LinkedList;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.github.cameltooling.idea.completion.property.CamelYamlPropertyKeyCompletion.enableCompletion;

/**
 * The {@link CompletionProvider} that gives the value of the options of main, components, languages and data formats for
 * yaml files.
 */
public class CamelYamlPropertyValueCompletion extends CamelPropertyValueCompletion {

    @Override
    protected @NotNull PsiElement getCompletionPosition(@NotNull CompletionParameters parameters) {
        return parameters.getPosition();
    }

    @Override
    protected boolean isEnabled(final Project project, final PsiFile file) {
        return enableCompletion(project, file);
    }

    @Override
    protected @Nullable String getPropertyKey(final PsiElement element) {
        final Deque<String> keys = new LinkedList<>();
        YAMLKeyValue parent = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        while (parent != null) {
            keys.addFirst(parent.getKeyText());
            parent = PsiTreeUtil.getParentOfType(parent, YAMLKeyValue.class);
        }
        return keys.isEmpty() ? null : String.join(".", keys);
    }
}
