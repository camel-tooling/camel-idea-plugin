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

import java.util.List;
import java.util.Map;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.idea.catalog.CamelCatalogService;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.EndpointOptionModel;
import org.apache.camel.idea.model.ModelHelper;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.idea.completion.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsContextPath;
import static org.apache.camel.idea.completion.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsQueryParameters;
import static org.apache.camel.idea.completion.CamelSmartCompletionEndpointValue.addSmartCompletionForSingleValue;

/**
 * Extension for supporting camel smart completion for camel options and values.
 */
public class CamelEndpointSmartCompletionExtension implements CamelCompletionExtension {

    private final boolean xmlMode;

    /**
     * Camel endpoint smart completion which works in Java or XML mode
     *
     * @param xmlMode <tt>true</tt> for XML mode, <tt>false</tt> for Java mode
     */
    public CamelEndpointSmartCompletionExtension(boolean xmlMode) {
        this.xmlMode = xmlMode;
    }

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull String[] query) {
        // it is a known Camel component

        String componentName = StringUtils.asComponentName(query[0]);

        // it is a known Camel component
        Project project = parameters.getOriginalFile().getManager().getProject();
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();

        String json = camelCatalog.componentJSonSchema(componentName);
        ComponentModel componentModel = ModelHelper.generateComponentModel(json, true);

        // grab all existing parameters
        String val = query[0];
        String suffix = query[1];
        String camelQuery = val;

        // camel catalog expects &amp; as & when it parses so replace all &amp; as &
        camelQuery = camelQuery.replaceAll("&amp;", "&");

        // strip up ending incomplete parameter
        if (camelQuery.endsWith("&") || camelQuery.endsWith("?")) {
            camelQuery = camelQuery.substring(0, camelQuery.length() - 1);
        }

        Map<String, String> existing = null;
        try {
            existing = camelCatalog.endpointProperties(camelQuery);
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
            String name;
            if (xmlMode) {
                int pos1 = val.lastIndexOf("&amp;");
                int pos2 = val.lastIndexOf('?');
                if (pos1 > pos2) {
                    name = val.substring(pos1 + 5);
                } else {
                    name = val.substring(pos2 + 1);
                }
            } else {
                int pos = Math.max(val.lastIndexOf('&'), val.lastIndexOf('?'));
                name = val.substring(pos + 1);
            }
            name = name.substring(0, name.length() - 1); // remove =
            EndpointOptionModel endpointOption = componentModel.getEndpointOption(name);
            if (endpointOption != null) {
                answer = addSmartCompletionForSingleValue(parameters.getEditor(), val, suffix, endpointOption, xmlMode);
            }
        } else if (editQueryParameters) {
            // suggest a list of options for query parameters
            answer = addSmartCompletionSuggestionsQueryParameters(val, componentModel, existing, xmlMode);
        } else if (editContextPath) {
            // suggest a list of options for context-path
            answer = addSmartCompletionSuggestionsContextPath(val, componentModel, existing, xmlMode);
        }

        // are there any results then add them
        if (answer != null && !answer.isEmpty()) {
            resultSet.withPrefixMatcher(val).addAllElements(answer);
        }
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, String query[]) {
        // is this a possible Camel endpoint uri which we know
        String componentName = StringUtils.asComponentName(query[0]);
        Project project = parameters.getOriginalFile().getProject();
        if (!query[0].endsWith("{{") && componentName != null && ServiceManager.getService(project, CamelCatalogService.class).get().findComponentNames().contains(componentName)) {
            return true;
        }
        return false;
    }
}
