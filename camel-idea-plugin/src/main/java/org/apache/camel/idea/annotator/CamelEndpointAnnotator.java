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
package org.apache.camel.idea.annotator;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.idea.util.StringUtils.isEmpty;

/**
 * Validate Camel URI endpoint and simple expression and annotated the specific property to highlight the error in the editor
 */
public class CamelEndpointAnnotator extends AbstractCamelAnnotator {

    private static final Logger LOG = Logger.getInstance(CamelEndpointAnnotator.class);

    @Override
    boolean isEnabled() {
        return ServiceManager.getService(CamelPreferenceService.class).isRealTimeEndpointValidation();
    }

    /**
     * Validate endpoint options list aka properties. eg "timer:trigger?delay=1000&bridgeErrorHandler=true"
     * if the URI is not valid a error annotation is created and highlight the invalid value.
     */
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String uri) {
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

                extractMapValue(result, result.getInvalidBoolean(), uri, element, holder, new BooleanErrorMsg());
                extractMapValue(result, result.getInvalidEnum(), uri, element, holder, new EnumErrorMsg());
                extractMapValue(result, result.getInvalidInteger(), uri, element, holder, new IntegerErrorMsg());
                extractMapValue(result, result.getInvalidNumber(), uri, element, holder, new NumberErrorMsg());
                extractMapValue(result, result.getInvalidReference(), uri, element, holder, new ReferenceErrorMsg());
                extractSetValue(result, result.getUnknown(), uri, element, holder, new UnknownErrorMsg());
                extractSetValue(result, result.getNotConsumerOnly(), uri, element, holder, new NotConsumerOnlyErrorMsg());
                extractSetValue(result, result.getNotProducerOnly(), uri, element, holder, new NotProducerOnlyErrorMsg());
            } catch (Throwable e) {
                LOG.warn("Error validating Camel endpoint: " + uri, e);
            }
        }
    }

    private void extractSetValue(EndpointValidationResult result, Set<String> validationSet,
                                 String fromElement, PsiElement element, AnnotationHolder holder, CamelAnnotatorEndpointMessage msg) {
        if ((!result.isSuccess()) && validationSet != null) {

            for (String entry : validationSet) {
                String propertyValue = entry;

                int startIdxQueryParameters = fromElement.indexOf("?" + propertyValue);
                startIdxQueryParameters = (startIdxQueryParameters == -1) ? fromElement.indexOf("&" + propertyValue) : fromElement.indexOf("?");

                int propertyIdx = fromElement.indexOf(propertyValue, startIdxQueryParameters);
                int propertyLength = propertyValue.length();

                propertyIdx = IdeaUtils.isJavaLanguage(element) || IdeaUtils.isXmlLanguage(element) || IdeaUtils.isScalaLanguage(element) ? propertyIdx + 1 : propertyIdx;

                TextRange range = new TextRange(element.getTextRange().getStartOffset() + propertyIdx,
                    element.getTextRange().getStartOffset() + propertyIdx + propertyLength);

                if (msg.isWarnLevel()) {
                    holder.createWarningAnnotation(range, summaryErrorMessage(result, propertyValue, msg));
                } else {
                    holder.createErrorAnnotation(range, summaryErrorMessage(result, propertyValue, msg));
                }
            }
        }
    }

    private void extractMapValue(EndpointValidationResult result, Map<String, String> validationMap,
                                 String fromElement, @NotNull PsiElement element, @NotNull AnnotationHolder holder, CamelAnnotatorEndpointMessage msg) {
        if ((!result.isSuccess()) && validationMap != null) {

            for (Map.Entry<String, String> entry : validationMap.entrySet()) {
                String propertyValue = entry.getValue();
                String propertyKey = entry.getKey();
                int startIdxQueryParameters = fromElement.indexOf("?");

                int propertyIdx = fromElement.indexOf(propertyKey, startIdxQueryParameters);
                int startIdx = propertyIdx;

                int equalsSign = fromElement.indexOf("=", propertyIdx);

                if (equalsSign > 0) {
                    startIdx = equalsSign + 1;
                }

                int propertyLength = propertyValue.isEmpty() ? propertyKey.length() : propertyValue.length();
                propertyLength = element instanceof XmlToken ? propertyLength - 1 : propertyLength;

                startIdx = propertyValue.isEmpty() ? propertyIdx + 1 : fromElement.indexOf(propertyValue, startIdx) + 1;
                startIdx = IdeaUtils.isJavaLanguage(element) || IdeaUtils.isXmlLanguage(element) || IdeaUtils.isScalaLanguage(element) ? startIdx  : startIdx - 1;

                TextRange range = new TextRange(element.getTextRange().getStartOffset() + startIdx,
                    element.getTextRange().getStartOffset() + startIdx + propertyLength);
                holder.createErrorAnnotation(range, summaryErrorMessage(result, entry, msg));
            }
        }
    }

    private static class BooleanErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            boolean empty = entry.getValue() == null || entry.getValue().length() == 0;
            if (empty) {
                return "Empty boolean value";
            } else {
                return "Invalid boolean value: " + entry.getValue();
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
            String msg = "Invalid enum value: " + entry.getValue() + ". Possible values: " + str;
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
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return "Empty reference value";
            } else if (!entry.getValue().startsWith("#")) {
                return "Invalid reference value: " + entry.getValue() + " must start with #";
            } else {
                return "Invalid reference value: " + entry.getValue();
            }
        }
    }

    private static class IntegerErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return "Empty integer value";
            } else {
                return "Invalid integer value: " + entry.getValue();
            }
        }
    }

    private static class UnknownErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return "Unknown option";
        }
    }

    private static class NumberErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return "Empty number value";
            } else {
                return "Invalid number value: " + entry.getValue();
            }
        }
    }

    private static class NotConsumerOnlyErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return "Option not applicable in consumer only mode";
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
            return "Option not applicable in producer only mode";
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
