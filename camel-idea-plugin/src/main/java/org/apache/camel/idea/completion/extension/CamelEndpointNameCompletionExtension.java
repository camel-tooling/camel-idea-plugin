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
package org.apache.camel.idea.completion.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
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
import org.apache.camel.idea.reference.endpoint.CamelEndpoint;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Extension for supporting camel code completion for camel endpoint names (only for direct endpoints so far).
 */
public class CamelEndpointNameCompletionExtension implements CamelCompletionExtension {

    private final boolean xmlMode;

    public CamelEndpointNameCompletionExtension(boolean xmlMode) {
        this.xmlMode = xmlMode;
    }

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull String[] query) {
        String concatQuery = query[0];
        String suffix = query[1];
        String queryAtPosition = query[2];

        if (queryAtPosition.contains("?")) {
            //do not add completions when inside endpoint query params
            return;
        }

        String prefixValue = query[2];
        List<LookupElement> results = new ArrayList<>();

        PsiElement element = parameters.getPosition();

        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();
        if (camelIdeaUtils.isInsideCamelRoute(element, true) && camelIdeaUtils.isProducerEndpoint(element)) {
            results.addAll(getDirectEndpointSuggestions(module));
        }

        if (!results.isEmpty()) {
            resultSet.withPrefixMatcher(prefixValue).addAllElements(results);
            resultSet.stopHere();
        }
    }

    private List<LookupElement> getDirectEndpointSuggestions(Module module) {
        List<PsiElement> endpointDeclarations = CamelIdeaUtils.getService().findEndpointDeclarations(module, e -> true);
        return endpointDeclarations.stream()
            .map(this::createLookupElement)
            .collect(Collectors.toList());
    }

    @NotNull
    private LookupElement createLookupElement(PsiElement e) {
        PsiFile parentFile = e.getContainingFile();
        PsiClass parentClass = PsiTreeUtil.getParentOfType(e, PsiClass.class);

        CamelEndpoint endpoint = new CamelEndpoint(e.getText());
        return LookupElementBuilder.create(endpoint.getBaseUri())
            .withPresentableText(endpoint.getBaseUri())
            .withTypeText(parentClass == null ? parentFile.getName() : parentClass.getName())
            .withIcon(CamelPreferenceService.getService().getCamelIcon())
            .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE);
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, String[] query) {
        return true;
    }

}
