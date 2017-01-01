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
package org.apache.camel.idea;

import java.util.List;
import java.util.Map;
import javax.swing.*;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.EndpointOptionModel;
import org.apache.camel.idea.model.ModelHelper;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsContextPath;
import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsQueryParameters;
import static org.apache.camel.idea.CamelSmartCompletionEndpointValue.addSmartCompletionForSingleValue;

/**
 * Hook into the IDEA language completion system, to setup Camel smart completion.
 * Extend this class to define what it should re-act on when using smart completion
 */
public class CamelContributor extends CompletionContributor {

    public static final Icon CAMEL_ICON = IconLoader.getIcon("/icons/camel.png");

    private static final CamelCatalog CAMEL_CATALOG = new DefaultCamelCatalog(true);

    /**
     * Smart completion for Camel endpoints.
     */
    protected static class EndpointCompletion extends CompletionProvider<CompletionParameters> {

        private final CamelSmartCompletionPropertyPlaceholders smartCompletionPropertyPlaceholders;

        public EndpointCompletion(CamelSmartCompletionPropertyPlaceholders smartCompletionPropertyPlaceholders) {

            this.smartCompletionPropertyPlaceholders = smartCompletionPropertyPlaceholders;
        }

        public void addCompletions(@NotNull CompletionParameters parameters,
                                   ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet) {
            // is this a possible Camel endpoint uri which we know
            String[] tuple = parsePsiElement(parameters);
            String val = tuple[0];
            String suffix = tuple[1];

            if (val.endsWith("{{")) {
                smartCompletionPropertyPlaceholders.propertyPlaceholdersSmartCompletion(parameters, resultSet);
                return; //we are done
            }

            String componentName = StringUtils.asComponentName(val);
            if (componentName != null && CAMEL_CATALOG.findComponentNames().contains(componentName)) {

                // it is a known Camel component
                String json = CAMEL_CATALOG.componentJSonSchema(componentName);
                ComponentModel componentModel = ModelHelper.generateComponentModel(json, true);

                // grab all existing parameters
                String query = val;
                // strip up ending incomplete parameter
                if (query.endsWith("&") || query.endsWith("?")) {
                    query = query.substring(0, query.length() - 1);
                }

                Map<String, String> existing = null;
                try {
                    existing = CAMEL_CATALOG.endpointProperties(query);
                } catch (Exception e) {
                    // ignore
                }

                // are we editing an existing parameter value
                // or are we having a list of suggested parameters to choose among
                boolean editSingle = val.endsWith("=");
                boolean editQueryParameters = val.contains("?");
                boolean editContextPath = !editQueryParameters;

                List<LookupElement> answer = null;
                if (editSingle) {
                    // parameter name is before = and & or ?
                    int pos = Math.max(val.lastIndexOf('&'), val.lastIndexOf('?'));
                    String name = val.substring(pos + 1);
                    name = name.substring(0, name.length() - 1); // remove =
                    EndpointOptionModel endpointOption = componentModel.getEndpointOption(name);
                    if (endpointOption != null) {
                        answer = addSmartCompletionForSingleValue(parameters.getEditor(), val, suffix, endpointOption);
                    }
                } else if (editQueryParameters) {
                    // suggest a list of options for query parameters
                    answer = addSmartCompletionSuggestionsQueryParameters(val, componentModel, existing);
                } else if (editContextPath) {
                    // suggest a list of options for context-path
                    answer = addSmartCompletionSuggestionsContextPath(val, componentModel, existing);
                }

                // are there any results then add them
                if (answer != null && !answer.isEmpty()) {
                    resultSet.withPrefixMatcher(val).addAllElements(answer);
                }
            }
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

}
