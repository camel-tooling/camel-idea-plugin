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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelProjectPreferenceService;
import com.github.cameltooling.idea.service.KameletService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.icons.AllIcons;
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

        final IdeaUtils ideaUtils = IdeaUtils.getService();
        boolean caretAtEndOfLine = ideaUtils.isCaretAtEndOfLine(element);
        LOG.trace("Caret at end of line: " + caretAtEndOfLine);

        String[] queryParameter = ideaUtils.getQueryParameterAtCursorPosition(element);
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
        final Mode mode = Mode.getMode(componentName);
        Map<String, String> existing = null;
        try {
            existing = mode.endpointProperties(project, camelCatalog, componentName, concatQuery);
        } catch (Exception e) {
            LOG.warn("Error parsing Camel endpoint properties with url: " + queryAtPosition, e);
        }
        final ComponentModel componentModel = mode.componentModel(
            project, camelCatalog, componentName, concatQuery, CamelIdeaUtils.getService().isConsumerEndpoint(element)
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
                answer = addSmartCompletionSuggestionsContextPath(
                    queryAtPosition, componentModel, existing, element, mode.getContextPathComponentPredicate(),
                    mode.getContextPathOptionPredicate(), mode.getIconProvider(project)
                );
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

    /**
     * {@code Mode} defines all the specific methods needed to adapt the behavior of the class
     * {@link CamelEndpointSmartCompletionExtension} according to the requested component.
     */
    enum Mode {
        /**
         * The default mode corresponding to any component but Kamelet.
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

            @Override
            Predicate<ComponentModel> getContextPathComponentPredicate() {
                return component -> component
                    .getEndpointOptions()
                    .stream()
                    .filter(o -> "path".equals(o.getKind()) && o.getEnums() != null)
                    .count() == 1;
            }

            @Override
            Predicate<ComponentModel.EndpointOptionModel> getContextPathOptionPredicate() {
                return o -> o.getEnums() != null;
            }

            @Override
            Function<ComponentModel.EndpointOptionModel, Icon> getIconProvider(Project project) {
                return o -> AllIcons.Nodes.Enum;
            }
        },
        /**
         * The specific mode for the Kamelet component.
         */
        KAMELET {
            @Override
            public ComponentModel componentModel(Project project, CamelCatalog camelCatalog, String componentName,
                                                 String uri, boolean consumer) {
                // Rebuild the component model instead of getting it from the catalog as it will be altered
                // and the component model instance likely shared thanks to an internal cache.
                final String json = camelCatalog.componentJSonSchema(componentName);
                if (json == null) {
                    return null;
                }
                final ComponentModel componentModel = JsonMapper.generateComponentModel(json);
                final KameletService service = project.getService(KameletService.class);
                if (CamelProjectPreferenceService.getService(project).isOnlyShowKameletOptions()) {
                    componentModel.getEndpointOptions().clear();
                }
                // Add the list of name of Kamelets available according to the type of endpoint
                componentModel.getEndpointOptions().addAll(createKameletNameOptions(service, consumer));
                final String name = getKameletName(uri);
                if (!name.isEmpty()) {
                    final JSONSchemaProps props = service.getDefinition(name);
                    // Add the parameters defined in the Kamelet definition if any
                    if (props != null && props.getProperties() != null) {
                        final List<String> required = props.getRequired();
                        for (Map.Entry<String, JSONSchemaProps> entry : props.getProperties().entrySet()) {
                            final ComponentModel.EndpointOptionModel option = new ComponentModel.EndpointOptionModel();
                            final JSONSchemaProps schemaProps = entry.getValue();
                            option.setKind("parameter");
                            String nameProperty = entry.getKey();
                            option.setName(nameProperty);
                            option.setGroup(required.contains(nameProperty) ? "common" : "advanced");
                            option.setRequired(required.contains(nameProperty));
                            option.setDisplayName(schemaProps.getTitle());
                            option.setJavaType(schemaProps.getType());
                            option.setDescription(createDescription(schemaProps));
                            JsonNode defaultValue = schemaProps.getDefault();
                            if (defaultValue != null) {
                                option.setDefaultValue(defaultValue.asText());
                            }
                            option.setSecret("password".equalsIgnoreCase(schemaProps.getFormat()));
                            final List<JsonNode> values = schemaProps.getEnum();
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

            @Override
            Predicate<ComponentModel> getContextPathComponentPredicate() {
                return componentModel -> true;
            }

            @Override
            Predicate<ComponentModel.EndpointOptionModel> getContextPathOptionPredicate() {
                return option -> "KameletName".equals(option.getLabel());
            }

            @Override
            Function<ComponentModel.EndpointOptionModel, Icon> getIconProvider(Project project) {
                return o -> project.getService(KameletService.class).getIcon(o.getName());
            }

            /**
             * @param service the service from which the name of available Kamelets are retrieved.
             * @param consumer the flag indicating if the related endpoint is a consumer or not.
             * @return a list of options corresponding to Kamelets for consumer endpoint if {@code consumer} is
             * {@code true} otherwise with the list of Kamelets for producer endpoint.
             */
            private List<ComponentModel.EndpointOptionModel> createKameletNameOptions(KameletService service,
                                                                                      boolean consumer) {
                final List<ComponentModel.EndpointOptionModel> result = new ArrayList<>();
                for (String name : consumer ? service.getConsumerNames() : service.getProducerNames()) {
                    final JSONSchemaProps definition = service.getDefinition(name);
                    if (definition == null) {
                        LOG.trace("No definition could be found for the Kamelet " + name);
                        continue;
                    }
                    final ComponentModel.EndpointOptionModel option = new ComponentModel.EndpointOptionModel();
                    option.setName(name);
                    option.setLabel("KameletName");
                    option.setKind("path");
                    option.setDisplayName(definition.getTitle());
                    option.setType("string");
                    option.setJavaType("java.lang.String");
                    option.setDescription(definition.getDescription());
                    result.add(option);
                }
                return result;
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
         * @return the predicate allowing to know whether context path suggestions should be provided.
         */
        abstract Predicate<ComponentModel> getContextPathComponentPredicate();

        /**
         * @return the predicate allowing to know whether a {@link com.intellij.codeInsight.lookup.LookupElement} should
         * be added for a given context path option.
         */
        abstract Predicate<ComponentModel.EndpointOptionModel> getContextPathOptionPredicate();

        /**
         * @param project the project for which the icon needs to be retrieved.
         *
         * @return the function allowing to get the right icon to display for a given option.
         */
        abstract Function<ComponentModel.EndpointOptionModel, Icon> getIconProvider(Project project);

        /**
         * @param componentName the name of the component for which the mode is expected.
         * @return the mode corresponding to given component.
         */
        static Mode getMode(String componentName) {
            if ("kamelet".equals(componentName)) {
                return KAMELET;
            }
            return DEFAULT;
        }
    }
}
