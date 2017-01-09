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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.idea.util.IdeaUtils.isEmpty;

/**
 * Validate Camel URI endpoint and simple expression and annotated the specific property to highlight the error in the editor
 */
public class CamelEndpointAnnotator extends AbstractCamelAnnotator {

    /**
     * Validate endpoint options list aka properties. eg "timer:trigger?delay=1000&bridgeErrorHandler=true"
     * if the URI is not valid a error annotation is created and highlight the invalid value.
     */
    void validateEndpoint(@NotNull PsiElement element, @NotNull AnnotationHolder holder, String uri) {
        CamelCatalog catalogService = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get();
        EndpointValidationResult validateEndpointProperties = catalogService.validateEndpointProperties(uri.replaceAll("&amp;", "&"));
        extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidBoolean(), uri, element, holder, new BooleanErrorMsg());
        extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidEnum(), uri, element, holder, new EnumErrorMsg());
        extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidInteger(), uri, element, holder, new IntegerErrorMsg());
        extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidNumber(), uri, element, holder, new NumberErrorMsg());
        extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidReference(), uri, element, holder, new ReferenceErrorMsg());
        extractSetValue(validateEndpointProperties, validateEndpointProperties.getUnknown(), uri, element, holder, new UnknownErrorMsg());

    }

    private void extractSetValue(EndpointValidationResult validateEndpointProperties, Set<String> validationSet,
                                 String fromElement, PsiElement element, AnnotationHolder holder, CamelAnnotatorEndpointMessage msg) {
        if ((!validateEndpointProperties.isSuccess()) && validationSet != null) {

            for (String entry : validationSet) {
                String propertyValue = entry;

                int propertyIdx = fromElement.indexOf(propertyValue);
                int propertyLength = propertyValue.length();

                propertyIdx = IdeaUtils.isJavaLanguage(element) || IdeaUtils.isXmlLanguage(element) ? propertyIdx + 1 : propertyIdx;

                TextRange range = new TextRange(element.getTextRange().getStartOffset() + propertyIdx,
                    element.getTextRange().getStartOffset() + propertyIdx + propertyLength);
                holder.createErrorAnnotation(range, summaryErrorMessage(validateEndpointProperties, propertyValue, msg));
            }
        }
    }

    private void extractMapValue(EndpointValidationResult validateEndpointProperties, Map<String, String> validationMap,
                                 String fromElement, @NotNull PsiElement element, @NotNull AnnotationHolder holder, CamelAnnotatorEndpointMessage msg) {
        if ((!validateEndpointProperties.isSuccess()) && validationMap != null) {

            for (Map.Entry<String, String> entry : validationMap.entrySet()) {
                String propertyValue = entry.getValue();
                String propertyKey = entry.getKey();

                int propertyIdx = fromElement.indexOf(propertyKey);
                int startIdx = propertyIdx;

                int equalsSign = fromElement.indexOf("=", propertyIdx);

                if (equalsSign > 0) {
                    startIdx = equalsSign + 1;
                }

                int propertyLength = propertyValue.isEmpty() ? propertyKey.length() : propertyValue.length();
                propertyLength = element instanceof XmlToken ? propertyLength - 1 : propertyLength;

                startIdx = propertyValue.isEmpty() ? propertyIdx + 1 : fromElement.indexOf(propertyValue, startIdx) + 1;
                startIdx = IdeaUtils.isJavaLanguage(element) || IdeaUtils.isXmlLanguage(element) ? startIdx  : startIdx - 1;

                TextRange range = new TextRange(element.getTextRange().getStartOffset() + startIdx,
                    element.getTextRange().getStartOffset() + startIdx + propertyLength);
                holder.createErrorAnnotation(range, summaryErrorMessage(validateEndpointProperties, entry, msg));
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
            // for each invalid option build a reason message
            String returnMsg = "";
            if (result.getUnknown() != null) {
                for (String name : result.getUnknown()) {
                    if (name.equals(property)) {
                        if (result.getUnknownSuggestions() != null && result.getUnknownSuggestions().containsKey(name)) {
                            String[] suggestions = result.getUnknownSuggestions().get(name);
                            if (suggestions != null && suggestions.length > 0) {
                                String str = Arrays.asList(suggestions).toString();
                                returnMsg += "\nUnknown option. Did you mean: " + str;
                            } else {
                                returnMsg += "\nUnknown option";
                            }
                        } else {
                            returnMsg = "Unknown option";
                        }
                    }
                }
            }
            if (result.getRequired() != null) {
                for (String name : result.getRequired()) {
                    returnMsg += "\nMissing required option";
                }
            }
            return returnMsg;
        }
    }

    private static class NumberErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            boolean empty = isEmpty(entry.getValue());
            if (empty) {
                return "Empty number value";
            } else {
                return  "Invalid number value: " + entry.getValue();
            }
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
