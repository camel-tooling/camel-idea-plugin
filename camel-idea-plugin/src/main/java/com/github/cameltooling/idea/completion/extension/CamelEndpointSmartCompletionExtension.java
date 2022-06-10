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

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.KameletService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsContextPath;
import static com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsQueryParameters;
import static com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointValue.addSmartCompletionForEndpointValue;

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
        return ApplicationManager.getApplication().getService(IdeaUtils.class);
    }

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                               @NotNull CompletionResultSet resultSet, @NotNull String[] query) {
        boolean endsWithAmpQuestionMark = false;
        // it is a known Camel component
        final PsiElement element = parameters.getPosition();

        // grab all existing parameters
        String concatQuery = query[0];
        String suffix = query[1];
        String queryAtPosition = query[2];
        String prefixValue = query[2];
        // camel catalog expects &amp; as & when it parses so replace all &amp; as &
        concatQuery = concatQuery.replace("&amp;", "&");

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

        final String componentName = StringUtils.asComponentName(query[0]);
        final Project project = parameters.getOriginalFile().getManager().getProject();
        final CamelCatalog camelCatalog = project.getService(CamelCatalogService.class).get();
        final CatalogHandler catalogHandler = CatalogHandler.getInstance(componentName);
        Map<String, String> existing = null;
        try {
            existing = catalogHandler.endpointProperties(project, camelCatalog, componentName, concatQuery);
        } catch (Exception e) {
            LOG.warn("Error parsing Camel endpoint properties with url: " + queryAtPosition, e);
        }
        final ComponentModel componentModel = catalogHandler.componentModel(
            project, camelCatalog, componentName, concatQuery, getCamelIdeaUtils().isConsumerEndpoint(element)
        );
        if (componentModel == null) {
            return;
        }
        List<LookupElement> answer = null;
        if (editOptionValue) {
            String name = queryParameter[0].substring(1);
            ComponentModel.EndpointOptionModel endpointOption = componentModel.getEndpointOptions().stream().filter(
                o -> name.equals(o.getName()))
                .findFirst().orElse(null);
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
                answer = addSmartCompletionSuggestionsContextPath(queryAtPosition, componentModel, existing, element);
            }
        }
        // are there any results then add them
        if (!answer.isEmpty()) {
            resultSet.withPrefixMatcher(prefixValue).addAllElements(answer);
            resultSet.stopHere();
        }
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                           String[] query) {
        // is this a possible Camel endpoint uri which we know
        String componentName = StringUtils.asComponentName(query[0]);
        Project project = parameters.getOriginalFile().getProject();
        return !query[0].endsWith("{{") && componentName != null
            && project.getService(CamelCatalogService.class).get().findComponentNames().contains(componentName);
    }

    private static CamelIdeaUtils getCamelIdeaUtils() {
        return ApplicationManager.getApplication().getService(CamelIdeaUtils.class);
    }

    /**
     * {@code CatalogHandler} is a facade of a {@link CamelCatalog} allowing to easily adapt the behavior of the specific
     * methods that are used by the {@link CamelEndpointSmartCompletionExtension}.
     */
    enum CatalogHandler {
        /**
         * The default implementation of a catalog handler corresponding to any component but Kamelet.
         */
        DEFAULT {
            @Override
            public ComponentModel componentModel(Project project, CamelCatalog camelCatalog, String componentName,
                                                 String uri, boolean consumer) {
                return camelCatalog.componentModel(componentName);
            }

            @Override
            public Map<String, String> endpointProperties(Project project, CamelCatalog camelCatalog,
                                                          String componentName, String uri) throws URISyntaxException {
                return camelCatalog.endpointProperties(uri);
            }
        },
        /**
         * The specific implementation of a catalog handler for the Kamelet component.
         */
        KAMELET {
            @Override
            public ComponentModel componentModel(Project project, CamelCatalog camelCatalog, String componentName,
                                                 String uri, boolean consumer) {
                // Rebuild the component model instead of getting it from the catalog as it will be altered
                // and the component model instance likely shared thanks to an internal cache.
                String json = camelCatalog.componentJSonSchema(componentName);
                if (json == null) {
                    return null;
                }
                final ComponentModel componentModel = JsonMapper.generateComponentModel(json);
                final KameletService service = project.getService(KameletService.class);
                // Add the list of name of Kamelet available according to the type of endpoint
                componentModel.addEndpointOption(createKameletNameOption(service, consumer));
                final String name = getKameletName(uri);
                if (!name.isEmpty()) {
                    final JSONSchemaProps props = service.getDefinition(name);
                    // Add the parameters defined in the Kamelet definition if any
                    if (props != null && props.getProperties() != null) {
                        List<String> required = props.getRequired();
                        for (Map.Entry<String, JSONSchemaProps> entry : props.getProperties().entrySet()) {
                            ComponentModel.EndpointOptionModel option = new ComponentModel.EndpointOptionModel();
                            JSONSchemaProps schemaProps = entry.getValue();
                            option.setKind("parameter");
                            option.setGroup("common");
                            String nameProperty = entry.getKey();
                            option.setName(nameProperty);
                            option.setRequired(required.contains(nameProperty));
                            option.setDisplayName(schemaProps.getTitle());
                            option.setJavaType(schemaProps.getType());
                            option.setDescription(createDescription(schemaProps));
                            JsonNode defaultValue = schemaProps.getDefault();
                            if (defaultValue != null) {
                                option.setDefaultValue(defaultValue.asText());
                            }
                            option.setSecret("password".equalsIgnoreCase(schemaProps.getFormat()));
                            List<JsonNode> values = schemaProps.getEnum();
                            if (values != null && !values.isEmpty()) {
                                option.setEnums(values.stream().map(JsonNode::asText).collect(Collectors.toList()));
                            }
                            componentModel.addEndpointOption(option);
                        }
                    }
                }
                return componentModel;
            }

            /**
             * Generate the description of the option based on different values extracted from the given property
             * definition.
             * @param schemaProps the property definition from which the description is generated
             */
            private String createDescription(JSONSchemaProps schemaProps) {
                final StringBuilder descriptionSB = new StringBuilder();
                final String description = schemaProps.getDescription();
                if (description != null) {
                    descriptionSB.append(description);
                    if (!description.endsWith(".")) {
                        descriptionSB.append('.');
                    }
                    descriptionSB.append("<br/><br/>");
                }
                final String pattern = schemaProps.getPattern();
                if (pattern != null) {
                    descriptionSB.append("<b>Pattern:</b> <tt>");
                    descriptionSB.append(pattern);
                    descriptionSB.append("</tt><br/>");
                }
                final JsonNode example = schemaProps.getExample();
                if (example != null) {
                    descriptionSB.append("<b>Example:</b> <tt>");
                    descriptionSB.append(example.asText());
                    descriptionSB.append("</tt><br/>");
                }
                return descriptionSB.toString();
            }

            @Override
            public Map<String, String> endpointProperties(Project project, CamelCatalog camelCatalog,
                                                          String componentName, String uri) throws URISyntaxException {
                final Map<String, String> result = new HashMap<>(camelCatalog.endpointProperties(uri));
                result.putAll(camelCatalog.endpointLenientProperties(uri));
                return result;
            }

            /**
             * @param service the service from which the name of available Kamelets are retrieved.
             * @param consumer the flag indicating if the related endpoint is a consumer or not.
             * @return an option corresponding to the Kamelet name with the list of name of Kamelets for consumer
             * endpoint if {@code consumer} is {@code true} otherwise with the list of name of Kamelets for producer
             * endpoint.
             */
            private ComponentModel.EndpointOptionModel createKameletNameOption(KameletService service,
                                                                               boolean consumer) {
                ComponentModel.EndpointOptionModel option = new ComponentModel.EndpointOptionModel();
                option.setName("kameletName");
                option.setKind("path");
                option.setDisplayName("Kamelet Name");
                option.setType("string");
                option.setJavaType("java.lang.String");
                option.setDescription("The name of the Kamelet");
                option.setEnums(consumer ? service.getConsumerNames() : service.getProducerNames());
                return option;
            }

            /**
             * @param uri the uri from which the name of the Kamelet is extracted.
             * @return the name of the Kamelet that could be extracted from the uri.
             */
            private String getKameletName(String uri) {
                String contextPath = uri.substring("kamelet:".length());
                int index = contextPath.indexOf('?');
                if (index >= 0) {
                    contextPath = contextPath.substring(0, index);
                }
                index = contextPath.indexOf('/');
                return index == -1 ? contextPath : contextPath.substring(0, index);
            }
        };

        /**
         * Gives the model of the component corresponding to the given parameters.
         * @param project the project for which the model is expected.
         * @param camelCatalog the catalog from which the original model is retrieved.
         * @param componentName the name of the component for which the model is expected.
         * @param uri the current uri of the endpoint for which the model is expected
         * @param consumer a flag indicating if the related endpoint is a consumer or not.
         * @return the corresponding model of the given component.
         */
        abstract ComponentModel componentModel(Project project, CamelCatalog camelCatalog, String componentName,
                                               String uri, boolean consumer);

        /**
         * Parses the endpoint uri and constructs a key/value properties of each option
         *
         * @param project the project for which the endpoint uri should be parsed.
         * @param camelCatalog the catalog from which endpoint uri is parsed
         * @param componentName the name of the component for which the uri should be parsed.
         * @param uri the endpoint uri to parse.
         * @return properties as key value pairs of each endpoint option
         * @throws URISyntaxException if an error occurred while parsing the endpoint uri.
         */
        abstract Map<String, String> endpointProperties(Project project, CamelCatalog camelCatalog, String componentName,
                                                        String uri) throws URISyntaxException;

        /**
         * @param componentName the name of the component for which the catalog handler is expected.
         * @return the catalog handler corresponding to given component.
         */
        static CatalogHandler getInstance(String componentName) {
            if ("kamelet".equals(componentName)) {
                return KAMELET;
            }
            return DEFAULT;
        }
    }
}
