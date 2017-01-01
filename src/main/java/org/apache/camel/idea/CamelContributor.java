/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.completionproviders.CamelCompletionExtension;
import org.apache.camel.idea.completionproviders.CamelPropertiesSmartCompletionExtension;
import org.apache.camel.idea.completionproviders.JavaSmartCompletionExtension;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hook into the IDEA language completion system, to setup Camel smart completion.
 * Extend this class to define what it should re-act on when using smart completion
 */
public class CamelContributor extends CompletionContributor {

    public static final Icon CAMEL_ICON = IconLoader.getIcon("/icons/camel.png");

    private List<CamelCompletionExtension> camelCompletionExtensions = new ArrayList<>();

    CamelContributor() {
        addCompletionExtension(new JavaSmartCompletionExtension());
        addCompletionExtension(new CamelPropertiesSmartCompletionExtension());
    }

    /**
     * Smart completion for Camel endpoints.
     */
    static protected class EndpointCompletion extends CompletionProvider<CompletionParameters> {

        private final List<CamelCompletionExtension> camelCompletionExtensions;

        public EndpointCompletion(List<CamelCompletionExtension> camelCompletionExtensions) {
            this.camelCompletionExtensions = camelCompletionExtensions;
        }

        public void addCompletions(@NotNull CompletionParameters parameters,
                                   ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet) {
            String[] tuple = parsePsiElement(parameters);
            camelCompletionExtensions.stream()
                    .filter(p -> p.isValid(parameters,context,tuple))
                    .forEach(p -> p.addCompletions(parameters,context,resultSet,tuple));
        }
    }

    /**
     * Parse the PSI text {@link CompletionUtil#DUMMY_IDENTIFIER} and " character and remove them
     * @param parameters - completion parameter to parse
     * @return new string stripped for any {@link CompletionUtil#DUMMY_IDENTIFIER} and " character
     */
    @NotNull
    private static String[] parsePsiElement(@NotNull CompletionParameters parameters) {
        String val = parameters.getPosition().getText();
        String suffix = "";

        int len = CompletionUtil.DUMMY_IDENTIFIER.length();
        int hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER);
        if (hackIndex == -1) {
            hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED);
            len = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.length();
        }
        if (hackIndex > -1) {
            suffix = val.substring(hackIndex + len);
            val = val.substring(0, hackIndex);
        }

        if (val.startsWith("\"")) {
            val = val.substring(1);
        }
        if (val.endsWith("\"")) {
            val = val.substring(0, val.length() - 1);
        }
        if (suffix.endsWith("\"")) {
            suffix = suffix.substring(0, suffix.length() - 1);
        }
        return new String[]{val, suffix};
    }

    /**
     * Add additional completion extension to process when the
     * {@link CompletionProvider#addCompletions(CompletionParameters, ProcessingContext, CompletionResultSet)} is called
     */
    public void addCompletionExtension(CamelCompletionExtension provider){
        camelCompletionExtensions.add(provider);
    }

    public List<CamelCompletionExtension> getCamelCompletionExtensions() {
        return camelCompletionExtensions;
    }


}
