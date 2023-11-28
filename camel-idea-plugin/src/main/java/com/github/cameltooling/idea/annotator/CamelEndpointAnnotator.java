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
package com.github.cameltooling.idea.annotator;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.github.cameltooling.idea.reference.endpoint.direct.DirectEndpointReference;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.QueryUtils;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.XmlUtils;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlToken;
import com.intellij.ui.JBColor;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLQuotedText;

import static com.github.cameltooling.idea.util.StringUtils.isEmpty;

/**
 * Validate Camel URI endpoint and simple expression and annotated the specific property to highlight the error in the editor
 */
public class CamelEndpointAnnotator extends AbstractCamelAnnotator {

    private static final Logger LOG = Logger.getInstance(CamelEndpointAnnotator.class);

    /**
     * The specific text attributes in case of a deprecation.
     */
    private static final TextAttributes DEPRECATION_ATTRIBUTES;
    static {
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setEffectType(EffectType.STRIKEOUT);
        textAttributes.setEffectColor(JBColor.BLACK);
        DEPRECATION_ATTRIBUTES = textAttributes;
    }

    @Override
    boolean isEnabled() {
        return CamelPreferenceService.getService().isRealTimeEndpointValidation();
    }

    /**
     * Validate endpoint options list aka properties. eg "timer:trigger?delay=1000&bridgeErrorHandler=true"
     * if the URI is not valid an error annotation is created and highlight the invalid value.
     */
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String uri) {
        if (QueryUtils.isQueryContainingCamelComponent(element.getProject(), uri)) {
            IElementType type = element.getNode().getElementType();
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("Element %s of type: %s to validate endpoint uri: %s", element, type, uri));
            }

            final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();

            // skip special values such as configuring ActiveMQ brokerURL
            if (camelIdeaUtils.skipEndpointValidation(element)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Skipping element %s for validation with text: %s", element, uri));
                }
                return;
            }

            // camel catalog expects &amp; as & when it parses so replace all &amp; as &
            String camelQuery = IdeaUtils.getService().getInnerText(uri);
            if (camelQuery == null) {
                return;
            }
            camelQuery = camelQuery.replace("&amp;", "&");

            // strip up ending incomplete parameter
            if (camelQuery.endsWith("&") || camelQuery.endsWith("?")) {
                camelQuery = camelQuery.substring(0, camelQuery.length() - 1);
            }

            boolean stringFormat = camelIdeaUtils.isFromStringFormatEndpoint(element);
            if (stringFormat) {
                // if the node is fromF or toF, then replace all %X with {{%X}} as we cannot parse that value
                camelQuery = camelQuery.replace("%s", "{{%s}}");
                camelQuery = camelQuery.replace("%d", "{{%d}}");
                camelQuery = camelQuery.replace("%b", "{{%b}}");
            }

            boolean consumerOnly = camelIdeaUtils.isConsumerEndpoint(element);
            boolean producerOnly = camelIdeaUtils.isProducerEndpoint(element);

            if (producerOnly) {
                validateEndpointReference(element, camelQuery, holder);
            }

            try {
                CamelCatalog catalogService = element.getProject().getService(CamelCatalogService.class).get();
                EndpointValidationResult result = catalogService.validateEndpointProperties(camelQuery, false, consumerOnly, producerOnly);

                extractMapValue(result, result.getInvalidBoolean(), uri, element, holder, new BooleanErrorMsg());
                extractMapValue(result, result.getInvalidEnum(), uri, element, holder, new EnumErrorMsg());
                extractMapValue(result, result.getInvalidInteger(), uri, element, holder, new IntegerErrorMsg());
                extractMapValue(result, result.getInvalidDuration(), uri, element, holder, new DurationErrorMsg());
                extractMapValue(result, result.getInvalidNumber(), uri, element, holder, new NumberErrorMsg());
                extractMapValue(result, result.getInvalidReference(), uri, element, holder, new ReferenceErrorMsg());
                extractSetValue(result, result.getUnknown(), uri, element, holder, new UnknownErrorMsg(), false, null);
                extractSetValue(result, result.getLenient(), uri, element, holder, new LenientOptionMsg(CamelPreferenceService.getService().isHighlightCustomOptions()), true, null);
                extractSetValue(result, result.getNotConsumerOnly(), uri, element, holder, new NotConsumerOnlyErrorMsg(), false, null);
                extractSetValue(result, result.getNotProducerOnly(), uri, element, holder, new NotProducerOnlyErrorMsg(), false, null);
                extractSetValue(result, result.getDeprecated(), uri, element, holder, new DeprecatedErrorMsg(), true, DEPRECATION_ATTRIBUTES);
            } catch (Exception e) {
                LOG.warn(String.format("Error validating Camel endpoint: %s", uri), e);
            }
        }
    }

    private void validateEndpointReference(PsiElement element, String camelQuery, AnnotationHolder holder) {
        if (!IdeaUtils.getService().isJavaLanguage(element)) { //no need, unresolvable references in XML are already highlighted
            return;
        }
        if (CamelEndpoint.isDirectEndpoint(camelQuery)) { //only direct endpoints have references (for now)
            Arrays.stream(element.getReferences())
                .filter(DirectEndpointReference.class::isInstance)
                .map(DirectEndpointReference.class::cast)
                .findAny()
                .ifPresent(endpointReference -> {
                    ResolveResult[] targets = endpointReference.multiResolve(false);
                    if (targets.length == 0) {
                        TextRange range = endpointReference.getRangeInElement().shiftRight(element.getTextRange().getStartOffset());
                        holder.newAnnotation(HighlightSeverity.ERROR, String.format("Cannot find endpoint declaration: %s", endpointReference.getCanonicalText()))
                            .range(range).create();
                    }
                });
        }
    }

    /**
     * @return the {@link HighlightSeverity} corresponding to the given message
     */
    private HighlightSeverity getHighlightSeverity(CamelAnnotatorEndpointMessage<?> msg) {
        if (msg.isInfoLevel()) {
            return HighlightSeverity.INFORMATION;
        } else if (msg.isWarnLevel()) {
            return HighlightSeverity.WARNING;
        }
        return HighlightSeverity.ERROR;
    }

    private void extractSetValue(EndpointValidationResult result, Set<String> validationSet, String fromElement, PsiElement element,
                                 AnnotationHolder holder, CamelAnnotatorEndpointMessage<String> msg, boolean lenient,
                                 TextAttributes textAttributes) {
        if (validationSet != null && (lenient || !result.isSuccess())) {

            for (String propertyValue : validationSet) {

                int startIdxQueryParameters = fromElement.indexOf("?" + propertyValue);
                startIdxQueryParameters = (startIdxQueryParameters == -1) ? fromElement.indexOf("&" + propertyValue) : fromElement.indexOf("?");

                int propertyIdx = fromElement.indexOf(propertyValue, startIdxQueryParameters);
                int propertyLength = propertyValue.length();

                propertyIdx = useNormalIndex(element)  ? propertyIdx + 1 : propertyIdx;

                TextRange range = new TextRange(element.getTextRange().getStartOffset() + propertyIdx,
                    element.getTextRange().getStartOffset() + propertyIdx + propertyLength);

                AnnotationBuilder builder = holder.newAnnotation(getHighlightSeverity(msg), summaryMessage(result, propertyValue, msg));
                if (textAttributes != null) {
                    builder
                        .enforcedTextAttributes(textAttributes);
                }
                builder
                    .range(range).create();
            }
        }
    }

    private void extractMapValue(EndpointValidationResult result, Map<String, String> validationMap,
                                 String fromElement, @NotNull PsiElement element, @NotNull AnnotationHolder holder,
                                 CamelAnnotatorEndpointMessage<Map.Entry<String, String>> msg) {
        if (!result.isSuccess() && validationMap != null) {

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
                startIdx = useNormalIndex(element) ? startIdx  : startIdx - 1;

                TextRange range = new TextRange(element.getTextRange().getStartOffset() + startIdx,
                    element.getTextRange().getStartOffset() + startIdx + propertyLength);
                holder.newAnnotation(HighlightSeverity.ERROR, summaryMessage(result, entry, msg))
                        .range(range).create();
            }
        }
    }

    /**
     * Indicates whether the index to use should be normal or shifted.
     * @param element the element to test
     * @return {@code true} if the index to use should be the normal one, {@code false} otherwise.
     */
    private boolean useNormalIndex(@NotNull PsiElement element) {
        IdeaUtils ideaUtils = IdeaUtils.getService();
        XmlUtils xmlUtils = XmlUtils.getService();
        return ideaUtils.isJavaLanguage(element) || xmlUtils.isXmlLanguage(element)
            || ideaUtils.isYamlLanguage(element) && element instanceof YAMLQuotedText;
    }

    private static class BooleanErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                return "Empty boolean value";
            }
            return String.format("Invalid boolean value: %s", entry.getValue());
        }
    }

    private static class EnumErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            String name = entry.getKey();
            String[] choices = result.getInvalidEnumChoices().get(name);
            String defaultValue = result.getDefaultValues() != null ? result.getDefaultValues().get(entry.getKey()) : null;
            String choicesString = Arrays.asList(choices).toString();
            String msg = "Invalid enum value: " + entry.getValue() + ". Possible values: " + choicesString;
            if (result.getInvalidEnumChoices() != null) {
                String[] suggestions = result.getInvalidEnumChoices().get(name);
                if (suggestions != null && suggestions.length > 0) {
                    choicesString = Arrays.asList(suggestions).toString();
                    msg += ". Did you mean: " + choicesString;
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
            if (isEmpty(entry.getValue())) {
                return "Empty reference value";
            } else if (!entry.getValue().startsWith("#")) {
                return String.format("Invalid reference value: %s must start with #", entry.getValue());
            }
            return String.format("Invalid reference value: %s", entry.getValue());
        }
    }

    private static class IntegerErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            if (isEmpty(entry.getValue())) {
                return "Empty integer value";
            }
            return String.format("Invalid integer value: %s", entry.getValue());
        }
    }

    private static class DurationErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            if (isEmpty(entry.getValue())) {
                return "Empty duration value";
            }
            return String.format("Invalid duration value: %s", entry.getValue());
        }
    }

    private static class UnknownErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return "Unknown option";
        }
    }

    private static class DeprecatedErrorMsg implements CamelAnnotatorEndpointMessage<String> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return "Deprecated option";
        }

        @Override
        public boolean isWarnLevel() {
            // this is only a warning
            return true;
        }
    }

    private static class LenientOptionMsg implements CamelAnnotatorEndpointMessage<String> {

        private final boolean highlight;

        LenientOptionMsg(boolean highlight) {
            this.highlight = highlight;
        }

        @Override
        public String getErrorMessage(EndpointValidationResult result, String property) {
            return String.format("%s is a custom option that is not part of the Camel component", property);
        }

        @Override
        public boolean isInfoLevel() {
            return !highlight;
        }

        @Override
        public boolean isWarnLevel() {
            return highlight;
        }
    }

    private static class NumberErrorMsg implements CamelAnnotatorEndpointMessage<Map.Entry<String, String>> {
        @Override
        public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
            if (isEmpty(entry.getValue())) {
                return "Empty number value";
            }
            return String.format("Invalid number value: %s", entry.getValue());
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
     * A human-readable summary of the validation.
     *
     * @return the summary, or <tt>empty</tt> if no validation errors
     */
    private <T> String summaryMessage(EndpointValidationResult result, T entry, CamelAnnotatorEndpointMessage<T> msg) {
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
