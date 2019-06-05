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
package com.github.cameltooling.idea.completion.extension;

import java.util.List;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Simplified implementation of {@link CamelCompletionExtension}, useful when the extension does not need all the parameters,
 * context, etc.
 */
public abstract class SimpleCompletionExtension implements CamelCompletionExtension {

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                        @NotNull CompletionResultSet resultSet, @NotNull String[] query) {
        PsiElement element = parameters.getPosition();
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return;
        }

        List<LookupElement> results = findResults(element, getQueryAtPosition(query));
        if (!results.isEmpty()) {
            resultSet
                .withRelevanceSorter(CompletionSorter.emptySorter())
                .addAllElements(results);
            resultSet.stopHere();
        }
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, String[] query) {
        return isValid(parameters, context, getQueryAtPosition(query));
    }

    @NotNull
    private String getQueryAtPosition(String[] query) {
        String queryAtPosition = "";
        if (query.length > 2) {
            queryAtPosition = query[2];
        }
        return queryAtPosition;
    }

    protected abstract List<LookupElement> findResults(@NotNull PsiElement element, @NotNull String query);
    protected abstract boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull String query);


    @NotNull
    protected LookupElement createLookupElement(String lookupString, PsiElement e) {
        return createLookupElementBuilder(lookupString, e)
            .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE);
    }

    @NotNull
    protected LookupElementBuilder createLookupElementBuilder(String lookupString, PsiElement e) {
        PsiFile parentFile = e.getContainingFile();
        PsiClass parentClass = PsiTreeUtil.getParentOfType(e, PsiClass.class);

        return LookupElementBuilder.create(lookupString)
            .withPresentableText(lookupString)
            .withTailText(createLocationText(parentClass, parentFile), true)
            .withIcon(CamelPreferenceService.getService().getCamelIcon());
    }

    private String createLocationText(PsiClass clazz, PsiFile file) {
        String location = clazz == null ? file.getName() : clazz.getName();
        return " (" + location + ")";
    }

}
