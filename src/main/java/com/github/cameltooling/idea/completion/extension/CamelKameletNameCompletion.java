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
package com.github.cameltooling.idea.completion.extension;

import java.util.List;
import java.util.Map;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointOptions.addSmartCompletionContextPathSuggestions;

/**
 * {@code CamelKameletNameCompletion} is responsible for completing the name of a Camel Kamelet.
 */
public class CamelKameletNameCompletion extends CamelKameletCompletion {

    private final boolean consumerOnly;

    public CamelKameletNameCompletion(boolean consumerOnly) {
        this.consumerOnly = consumerOnly;
    }

    @Override
    protected @NotNull String getURIPrefix(PsiElement element) {
        return "kamelet:";
    }

    @Override
    protected boolean isConsumerOnly(Project project, PsiElement element) {
        return consumerOnly;
    }

    @Override
    protected List<LookupElement> suggestions(@NotNull ComponentModel componentModel, @NotNull Project project,
                                              @NotNull PsiElement element, @NotNull Editor editor, String val) {
        return addSmartCompletionContextPathSuggestions(
            "", componentModel, Map.of(), mode.getContextPathComponentPredicate(),
            mode.getContextPathOptionPredicate(), mode.getIconProvider(project)
        );
    }
}
