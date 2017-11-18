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
package org.apache.camel.idea.completion.contributor;

import java.util.ArrayList;
import java.util.List;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.completion.extension.CamelCompletionExtension;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Hook into the IDEA language completion system, to setup Camel smart completion.
 * Extend this class to define what it should re-act on when using smart completion
 */
public abstract class CamelContributor extends CompletionContributor {

    private final List<CamelCompletionExtension> camelCompletionExtensions = new ArrayList<>();

    CamelContributor() {
    }

    /**
     * Smart completion for Camel endpoints.
     */
    protected static class EndpointCompletion extends CompletionProvider<CompletionParameters> {

        private final List<CamelCompletionExtension> camelCompletionExtensions;

        EndpointCompletion(List<CamelCompletionExtension> camelCompletionExtensions) {
            this.camelCompletionExtensions = camelCompletionExtensions;
        }

        public void addCompletions(@NotNull CompletionParameters parameters,
                                   ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet) {
            if (ServiceManager.getService(parameters.getOriginalFile().getProject(), CamelService.class).isCamelPresent()) {
                String[] tuple = parsePsiElement(parameters);
                camelCompletionExtensions.stream()
                    .filter(p -> p.isValid(parameters, context, tuple))
                    .forEach(p -> p.addCompletions(parameters, context, resultSet, tuple));
            }
        }
    }

    /**
     * Parse the PSI text {@link CompletionUtil#DUMMY_IDENTIFIER} and " character and remove them.
     * <p/>
     * This implementation support Java literal expressions and XML attributes where you can define Camel endpoints.
     *
     * @param parameters - completion parameter to parse
     * @return new string stripped for any {@link CompletionUtil#DUMMY_IDENTIFIER} and " character
     */
    @NotNull
    private static String[] parsePsiElement(@NotNull CompletionParameters parameters) {
        PsiElement element = parameters.getPosition();

        String val = getIdeaUtils().extractTextFromElement(element, true, true, true);
        if (val == null || val.isEmpty()) {
            return new String[]{"", ""};
        }

        String valueAtPosition = getIdeaUtils().extractTextFromElement(element, true, false, true);

        String suffix = "";

        // okay IDEA folks its not nice, in groovy the dummy identifier is using lower case i in intellij
        // so we need to lower case it all
        String hackVal = valueAtPosition.toLowerCase();
        int len = CompletionUtil.DUMMY_IDENTIFIER.length();
        int hackIndex = hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER.toLowerCase());
        //let's scrup the data for any Intellij stuff
        val = val.replace(CompletionUtil.DUMMY_IDENTIFIER, "");
        if (hackIndex == -1) {
            val = val.replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "");
            hackIndex = hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.toLowerCase());
            len = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.length();
        }

        if (hackIndex > -1) {
            suffix = valueAtPosition.substring(hackIndex + len);
            valueAtPosition = valueAtPosition.substring(0, hackIndex);
        }

        return new String[]{val, suffix, valueAtPosition};
    }

    /**
     * Add additional completion extension to process when the
     * {@link CompletionProvider#addCompletions(CompletionParameters, ProcessingContext, CompletionResultSet)} is called
     */
    void addCompletionExtension(CamelCompletionExtension provider) {
        camelCompletionExtensions.add(provider);
    }

    List<CamelCompletionExtension> getCamelCompletionExtensions() {
        return camelCompletionExtensions;
    }

    /**
     * Checks if its a file of expect type
     */
    static PsiFilePattern.Capture<PsiFile> matchFileType(final String... extensions) {
        return new PsiFilePattern.Capture<>(new InitialPatternCondition<PsiFile>(PsiFile.class) {
            @Override
            public boolean accepts(@Nullable Object o, ProcessingContext context) {
                    if (o instanceof PsiFile) {
                    return getIdeaUtils().isFromFileType((PsiElement) o, extensions);
                }
                return false;
            }
        });
    }

    private static IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

}
