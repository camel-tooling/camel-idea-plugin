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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.EndpointOptionModel;
import org.apache.camel.idea.model.ModelHelper;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.util.IdeaUtils;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import static org.apache.camel.idea.completion.endpoint.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsContextPath;
import static org.apache.camel.idea.completion.endpoint.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsQueryParameters;
import static org.apache.camel.idea.completion.endpoint.CamelSmartCompletionEndpointValue.addSmartCompletionForEndpointValue;

/**
 * Extension for supporting camel smart completion for camel options and values.
 */
public class CamelEndpointSmartCompletionExtension implements CamelCompletionExtension {

    private static final Logger LOG = Logger.getInstance(CamelEndpointSmartCompletionExtension.class);

    private final boolean xmlMode;


    /**
     * Camel endpoint smart completion which works in Java or XML mode
     *
     * @param xmlMode <tt>true</tt> for XML mode, <tt>false</tt> for Java mode
     */
    public CamelEndpointSmartCompletionExtension(boolean xmlMode) {
        this.xmlMode = xmlMode;
    }

    public IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull String[] query) {
        boolean endsWithAmpQuestionMark = false;
        // it is a known Camel component
        String componentName = StringUtils.asComponentName(query[0]);

        // it is a known Camel component
        Project project = parameters.getOriginalFile().getManager().getProject();
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();

        String json = camelCatalog.componentJSonSchema(componentName);
        ComponentModel componentModel = ModelHelper.generateComponentModel(json, true);
        final PsiElement element = parameters.getPosition();

        // grab all existing parameters
        String concatQuery = query[0];
        String suffix = query[1];
        String queryAtPosition =  query[2];
        String prefixValue =  query[2];
        // camel catalog expects &amp; as & when it parses so replace all &amp; as &
        concatQuery = concatQuery.replaceAll("&amp;", "&");

        boolean editQueryParameters = concatQuery.contains("?");

        // strip up ending incomplete parameter
        if (queryAtPosition.endsWith("&") || queryAtPosition.endsWith("?")) {
            endsWithAmpQuestionMark = true;
            queryAtPosition = queryAtPosition.substring(0, queryAtPosition.length() - 1);
        }

        // strip up ending incomplete parameter
        if (concatQuery.endsWith("&") || concatQuery.endsWith("?")) {
            concatQuery = concatQuery.substring(0, concatQuery.length() - 1);
        }

        Map<String, String> existing = null;
        try {
            existing = camelCatalog.endpointProperties(concatQuery);
        } catch (Exception e) {
            LOG.warn("Error parsing Camel endpoint properties with url: " + queryAtPosition, e);
        }

        // are we editing an existing parameter value
        // or are we having a list of suggested parameters to choose among

        boolean caretAtEndOfLine = getIdeaUtils().isCaretAtEndOfLine(element);
        LOG.trace("Caret at end of line: " + caretAtEndOfLine);

        String[] queryParameter = getIdeaUtils().getQueryParameterAtCursorPosition(element);
        String optionValue = queryParameter[1];


        // a bit complex to figure out whether to edit the endpoint value or not
        boolean editOptionValue = false;
        if (endsWithAmpQuestionMark) {
            // should not edit value but suggest a new option instead
            editOptionValue = false;
        } else {
            if ("".equals(optionValue)) {
                // empty value so must edit
                editOptionValue = true;
            } else if (StringUtils.isNotEmpty(optionValue) && !caretAtEndOfLine) {
                // has value and cursor not at end of line so must edit
                editOptionValue = true;
            }
        }
        LOG.trace("Add new option: " + !editOptionValue);
        LOG.trace("Edit option value: " + editOptionValue);

        List<LookupElement> answer = null;
        if (editOptionValue) {
            EndpointOptionModel endpointOption = componentModel.getEndpointOption(queryParameter[0].substring(1));
            if (endpointOption != null) {
                answer = addSmartCompletionForEndpointValue(parameters.getEditor(), queryAtPosition, suffix, endpointOption, element, xmlMode);
            }
        }
        if (answer == null) {
            if (editQueryParameters) {
                // suggest a list of options for query parameters
                answer = addSmartCompletionSuggestionsQueryParameters(query, componentModel, existing, xmlMode, element, parameters.getEditor());
            } else {
                // suggest a list of options for context-path
                answer = addSmartCompletionSuggestionsContextPath(queryAtPosition, componentModel, existing, xmlMode, element);
            }
        }
        // are there any results then add them
        if (answer != null && !answer.isEmpty()) {
            resultSet.withPrefixMatcher(prefixValue).addAllElements(answer);
            resultSet.stopHere();
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
