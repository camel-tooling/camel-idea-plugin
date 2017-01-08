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
import java.util.LinkedHashMap;
import java.util.Map;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.catalog.SimpleValidationResult;
import org.apache.camel.idea.catalog.CamelCatalogService;
import org.apache.camel.idea.util.CamelService;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.idea.util.IdeaUtils.isEmpty;


/**
 * Validate Camel URI endpoint and simple expression and annotated the specific property
 * or simple expression to highlight the error on the editor
 */
public class CamelAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            String fromElement = IdeaUtils.extractTextFromElement(element);
            if (fromElement != null && !fromElement.isEmpty() && !fromElement.trim().endsWith("&") && !fromElement.trim().endsWith("?")
                && !fromElement.trim().endsWith("{{") && !fromElement.trim().endsWith("=")
                && !fromElement.trim().endsWith("${")) {
                validateEndpointProperties(element, holder, fromElement);
                //Comment out until we solve the classloader issue.
                //validateSimpleExpression(element, holder, fromElement);
            }
        }
    }

    /**
     * Validate endpoint options list aka properties. eg "timer:trigger?delay=1000&bridgeErrorHandler=true"
     * if the URI is not valid a error annotation is created and highlight the invalid value.
     */
    private void validateEndpointProperties(@NotNull PsiElement element, @NotNull AnnotationHolder holder, String fromElement) {
        if ((fromElement.contains("?") || fromElement.contains("&")) && IdeaUtils.isConsumerEndpoint(element)) {
            CamelCatalog catalogService = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get();
            EndpointValidationResult validateEndpointProperties = catalogService.validateEndpointProperties(fromElement);
            extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidBoolean(), fromElement, element, holder);
            extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidEnum(), fromElement, element, holder);
            extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidInteger(), fromElement, element, holder);
            extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidNumber(), fromElement, element, holder);
            extractMapValue(validateEndpointProperties, validateEndpointProperties.getInvalidReference(), fromElement, element, holder);
        }
    }

    // TODO : Solve classloader issue class not found when CamelCatalog access a camel core java file.
    // The problem is releated to the CamelCatalog is using the plugin classloader, but the Camel core
    // exits on the project/module classloader
    /**
     * Validate simple expression. eg "simple("${body}"
     * if the expression is not valid a error annotation is created and highlight the invalid value.
     */
    private void validateSimpleExpression(@NotNull PsiElement element, @NotNull AnnotationHolder holder, String fromElement) {
        if (fromElement.contains("${") &&  (IdeaUtils.isCamelRouteSimpleExpression(element))) {
            CamelCatalog catalogService = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get();
            SimpleValidationResult validateSimpleResult = catalogService.validateSimpleExpression(fromElement);
            if (!validateSimpleResult.isSuccess()) {
                String error = validateSimpleResult.getError();
                int propertyIdx = fromElement.indexOf("simple");
                int propertyLength = fromElement.length() + 8;
                TextRange range = new TextRange(element.getTextRange().getStartOffset() + propertyIdx,
                    element.getTextRange().getStartOffset() + propertyIdx + propertyLength);
                holder.createErrorAnnotation(range, error);
            }

        }
    }

    private void extractMapValue(EndpointValidationResult validateEndpointProperties, Map<String, String> validationMap,
                                 String fromElement, @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if ((!validateEndpointProperties.isSuccess()) && validationMap != null) {
            String propertyValue = validationMap.values().iterator().next();
            String propertyKey = validationMap.keySet().iterator().next();
            int propertyIdx = propertyValue.isEmpty() ? fromElement.indexOf(propertyKey) + 1 : fromElement.indexOf(propertyValue) + 1;
            int propertyLength = propertyValue.isEmpty() ? propertyKey.length() : propertyValue.length();
            TextRange range = new TextRange(element.getTextRange().getStartOffset() + propertyIdx,
                element.getTextRange().getStartOffset() + propertyIdx + propertyLength);
            holder.createErrorAnnotation(range, summaryErrorMessage(validateEndpointProperties));
        }
    }

    /**
     * A human readable summary of the validation errors.
     *
     * @return the summary, or <tt>null</tt> if no validation errors
     */
    private String summaryErrorMessage(EndpointValidationResult result) {

        if (result.getIncapable() != null) {
            return "\tIncapable of parsing uri: " + result.getIncapable();
        } else if (result.getSyntaxError() != null) {
            return "\tSyntax error: " + result.getSyntaxError();
        } else if (result.getUnknownComponent() != null) {
            return "\tUnknown component: " + result.getUnknownComponent();
        }

        // for each invalid option build a reason message
        Map<String, String> options = new LinkedHashMap<>();
        if (result.getUnknown() != null) {
            for (String name : result.getUnknown()) {
                if (result.getUnknownSuggestions() != null && result.getUnknownSuggestions().containsKey(name)) {
                    String[] suggestions = result.getUnknownSuggestions().get(name);
                    if (suggestions != null && suggestions.length > 0) {
                        String str = Arrays.asList(suggestions).toString();
                        options.put(name, "Unknown option. Did you mean: " + str);
                    } else {
                        options.put(name, "Unknown option");
                    }
                } else {
                    options.put(name, "Unknown option");
                }
            }
        }
        if (result.getRequired() != null) {
            for (String name : result.getRequired()) {
                options.put(name, "Missing required option");
            }
        }
        if (result.getInvalidEnum() != null) {
            for (Map.Entry<String, String> entry : result.getInvalidEnum().entrySet()) {
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

                options.put(entry.getKey(), msg);
            }
        }
        if (result.getInvalidReference() != null) {
            for (Map.Entry<String, String> entry : result.getInvalidReference().entrySet()) {
                boolean empty = isEmpty(entry.getValue());
                if (empty) {
                    options.put(entry.getKey(), "Empty reference value");
                } else if (!entry.getValue().startsWith("#")) {
                    options.put(entry.getKey(), "Invalid reference value: " + entry.getValue() + " must start with #");
                } else {
                    options.put(entry.getKey(), "Invalid reference value: " + entry.getValue());
                }
            }
        }
        if (result.getInvalidBoolean() != null) {
            for (Map.Entry<String, String> entry : result.getInvalidBoolean().entrySet()) {
                boolean empty = entry.getValue() == null || entry.getValue().length() == 0;
                if (empty) {
                    options.put(entry.getKey(), "Empty boolean value");
                } else {
                    options.put(entry.getKey(), "Invalid boolean value: " + entry.getValue());
                }
            }
        }
        if (result.getInvalidInteger() != null) {
            for (Map.Entry<String, String> entry : result.getInvalidInteger().entrySet()) {
                boolean empty = isEmpty(entry.getValue());
                if (empty) {
                    options.put(entry.getKey(), "Empty integer value");
                } else {
                    options.put(entry.getKey(), "Invalid integer value: " + entry.getValue());
                }
            }
        }
        if (result.getInvalidNumber() != null) {
            for (Map.Entry<String, String> entry : result.getInvalidNumber().entrySet()) {
                boolean empty = isEmpty(entry.getValue());
                if (empty) {
                    options.put(entry.getKey(), "Empty number value");
                } else {
                    options.put(entry.getKey(), "Invalid number value: " + entry.getValue());
                }
            }
        }

        // build the human error summary
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> option : options.entrySet()) {
            String out = option.getValue();
            sb.append(out);
        }

        return sb.toString();
    }
}
