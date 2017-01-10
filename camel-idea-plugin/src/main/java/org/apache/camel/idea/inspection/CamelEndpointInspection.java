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
package org.apache.camel.idea.inspection;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.idea.annotator.CamelAnnotatorEndpointMessage;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.IdeaUtils;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.util.StringUtils.isEmpty;

public class CamelEndpointInspection extends LocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(CamelEndpointInspection.class);

    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Apache Camel";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Inspect Camel endpoints";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "InpsectCamelEndpoints";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Inspects all Camel endpoints";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
                    String text = IdeaUtils.extractTextFromElement(element, false);
                    if (!StringUtils.isEmpty(text)) {
                        validateText(element, holder, text, isOnTheFly);
                    }
                }
            }
        };
    }

    /**
     * Validate endpoint options list aka properties. eg "timer:trigger?delay=1000&bridgeErrorHandler=true"
     * if the URI is not valid a error annotation is created and highlight the invalid value.
     */
    private void validateText(@NotNull PsiElement element, final @NotNull ProblemsHolder holder, @NotNull String uri, boolean isOnTheFly) {
        if (IdeaUtils.isQueryContainingCamelComponent(element.getProject(), uri)) {
            CamelCatalog catalogService = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get();

            // camel catalog expects &amp; as & when it parses so replace all &amp; as &
            String camelQuery = uri;
            camelQuery = camelQuery.replaceAll("&amp;", "&");

            // strip up ending incomplete parameter
            if (camelQuery.endsWith("&") || camelQuery.endsWith("?")) {
                camelQuery = camelQuery.substring(0, camelQuery.length() - 1);
            }

            boolean consumerOnly = IdeaUtils.isConsumerEndpoint(element);
            boolean producerOnly = IdeaUtils.isProducerEndpoint(element);

            try {
                EndpointValidationResult result = catalogService.validateEndpointProperties(camelQuery, false, consumerOnly, producerOnly);

                extractMapValue(result, result.getInvalidBoolean(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.BooleanErrorMsg());
                extractMapValue(result, result.getInvalidEnum(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.EnumErrorMsg());
                extractMapValue(result, result.getInvalidInteger(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.IntegerErrorMsg());
                extractMapValue(result, result.getInvalidNumber(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.NumberErrorMsg());
                extractMapValue(result, result.getInvalidReference(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.ReferenceErrorMsg());
                extractSetValue(result, result.getUnknown(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.UnknownErrorMsg());
                extractSetValue(result, result.getNotConsumerOnly(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.NotConsumerOnlyErrorMsg());
                extractSetValue(result, result.getNotProducerOnly(), uri, element, holder, isOnTheFly, new CamelEndpointInspection.NotProducerOnlyErrorMsg());
            } catch (Throwable e) {
                LOG.warn("Error inspecting Camel endpoint: " + uri, e);
            }
        }
    }

    private void extractSetValue(EndpointValidationResult result, Set<String> validationSet,
                                 String fromElement, PsiElement element, final @NotNull ProblemsHolder holder,
                                 boolean isOnTheFly, CamelAnnotatorEndpointMessage msg) {

        if ((!result.isSuccess()) && validationSet != null) {
            for (String entry : validationSet) {
                String desc = summaryErrorMessage(result, entry, msg);
                holder.registerProblem(element, desc);
            }
        }
    }

    private void extractMapValue(EndpointValidationResult result, Map<String, String> validationMap,
                                 String fromElement, @NotNull PsiElement element, final @NotNull ProblemsHolder holder,
                                 boolean isOnTheFly, CamelAnnotatorEndpointMessage msg) {

        if ((!result.isSuccess()) && validationMap != null) {
            for (Map.Entry<String, String> entry : validationMap.entrySet()) {
                String desc = summaryErrorMessage(result, entry, msg);
                holder.registerProblem(element, desc);
            }
        }
    }

    private static class BooleanErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            String name = entry.getKey();
            boolean empty = entry.getValue() == null || entry.getValue().length() == 0;
            if (empty) {
                return name + " has empty boolean value";
            } else {
                return name + " has invalid boolean value: " + entry.getValue();
            }
        }
    }

    private static class EnumErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            String name = entry.getKey();
            String[] choices = result.getInvalidEnumChoices().get(name);
            String defaultValue = result.getDefaultValues() != null ? result.getDefaultValues().get(entry.getKey()) : null;
            String str = Arrays.asList(choices).toString();
            String msg = name + " has invalid enum value: " + entry.getValue() + ". Possible values: " + str;
            if (result.getInvalidEnumChoices() != null) {
                String[] suggestions = result.getInvalidEnumChoices().get(name);
                if (suggestions != null && suggestions.length > 0) {
                    str = Arrays.asList(suggestions).toString();
                    msg += ". Did you mean: " + str;
                }
            }
            if (defaultValue != null) {
                msg += ". Default value: " + defaultValue;
            }
            return msg;
        }
    }

    private static class ReferenceErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            String name = entry.getKey();
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return name + " has empty reference value";
            } else if (!entry.getValue().startsWith("#")) {
                return name + " has invalid reference value: " + entry.getValue() + " must start with #";
            } else {
                return name + " has invalid reference value: " + entry.getValue();
            }
        }
    }

    private static class IntegerErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            String name = entry.getKey();
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return name + " is empty integer value";
            } else {
                return name + " is invalid integer value: " + entry.getValue();
            }
        }
    }

    private static class UnknownErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            // for each invalid option build a reason message
            String returnMsg = "";
            if (result.getUnknown() != null) {
                for (String name : result.getUnknown()) {
                    if (name.equals(property)) {
                        if (result.getUnknownSuggestions() != null && result.getUnknownSuggestions().containsKey(name)) {
                            String[] suggestions = result.getUnknownSuggestions().get(name);
                            if (suggestions != null && suggestions.length > 0) {
                                String str = Arrays.asList(suggestions).toString();
                                returnMsg += name + " is unknown option. Did you mean: " + str;
                            } else {
                                returnMsg += name + " is unknown option";
                            }
                        } else {
                            returnMsg = name + " is unknown option";
                        }
                    }
                }
            }
            if (result.getRequired() != null) {
                for (String name : result.getRequired()) {
                    returnMsg += name + " is required";
                }
            }
            return returnMsg;
        }
    }

    private static class NumberErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            String name = entry.getKey();
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return name + " has empty numeric value";
            } else {
                return name + " has invalid numeric value: " + entry.getValue();
            }
        }
    }

    private static class NotConsumerOnlyErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return property + " is not applicable in consumer only mode";
        }

        @Override
        public boolean isWarnLevel() {
            // this is only a warning
            return true;
        }
    }

    private static class NotProducerOnlyErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return property + " is not applicable in producer only mode";
        }

        @Override
        public boolean isWarnLevel() {
            // this is only a warning
            return true;
        }
    }

    /**
     * A human readable summary of the validation errors.
     *
     * @return the summary, or <tt>empty</tt> if no validation errors
     */
    @SuppressWarnings("unchecked")
    private <T> String summaryErrorMessage(EndpointValidationResult result, T entry, CamelAnnotatorEndpointMessage msg) {
        if (result.getIncapable() != null) {
            return "Incapable of parsing uri: " + result.getIncapable();
        } else if (result.getSyntaxError() != null) {
            return "Syntax error: " + result.getSyntaxError();
        } else if (result.getUnknownComponent() != null) {
            return "Unknown component: " + result.getUnknownComponent();
        }

        return msg.getErrorMessage(result, entry);
    }

}
