package org.apache.camel.idea.completionproviders;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.idea.StringUtils;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.EndpointOptionModel;
import org.apache.camel.idea.model.ModelHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsContextPath;
import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsQueryParameters;
import static org.apache.camel.idea.CamelSmartCompletionEndpointValue.addSmartCompletionForSingleValue;

/**
 * Extension for supporting camel smart completion for camel options and values.
 */
public class JavaSmartCompletionExtension implements CamelCompletionExtension {

    private static final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull String query) {
        // it is a known Camel component
        String componentName = StringUtils.asComponentName(query);

        // it is a known Camel component
        String json = camelCatalog.componentJSonSchema(componentName);
        ComponentModel componentModel = ModelHelper.generateComponentModel(json, true);

        // grab all existing parameters
        String camelQuery = query;
        // strip up ending incomplete parameter
        if (camelQuery.endsWith("&") || camelQuery.endsWith("?")) {
            camelQuery = camelQuery.substring(0, camelQuery.length() - 1);
        }

        Map<String, String> existing = null;
        try {
            existing = camelCatalog.endpointProperties(query);
        } catch (Exception e) {
            // ignore
        }

        // are we editing an existing parameter value
        // or are we having a list of suggested parameters to choose among
        boolean editSingle = query.endsWith("=");
        boolean editQueryParameters = query.contains("?");
        boolean editContextPath = !editQueryParameters;

        List<LookupElement> answer = null;
        if (editSingle) {
            // parameter name is before = and & or ?
            int pos = Math.max(query.lastIndexOf('&'), query.lastIndexOf('?'));
            String name = query.substring(pos + 1);
            name = name.substring(0, name.length() - 1); // remove =
            EndpointOptionModel endpointOption = componentModel.getEndpointOption(name);
            if (endpointOption != null) {
                answer = addSmartCompletionForSingleValue(query, endpointOption);
            }
        } else if (editQueryParameters) {
            // suggest a list of options for query parameters
            answer = addSmartCompletionSuggestionsQueryParameters(query, componentModel, existing);
        } else if (editContextPath) {
            // suggest a list of options for context-path
            answer = addSmartCompletionSuggestionsContextPath(query, componentModel, existing);
        }

        // are there any results then add them
        if (answer != null && !answer.isEmpty()) {
            resultSet.withPrefixMatcher(query).addAllElements(answer);
        }
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, String query) {
        // is this a possible Camel endpoint uri which we know
        String componentName = StringUtils.asComponentName(query);
        if (!query.endsWith("{{") && componentName != null && camelCatalog.findComponentNames().contains(componentName)) {
            return true;
        }
        return false;
    }
}
