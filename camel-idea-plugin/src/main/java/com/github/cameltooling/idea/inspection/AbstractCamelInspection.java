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
package com.github.cameltooling.idea.inspection;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import com.github.cameltooling.idea.annotator.CamelAnnotatorEndpointMessage;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.service.QueryUtils;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.catalog.LanguageValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static com.github.cameltooling.idea.util.StringUtils.isEmpty;

/**
 * Base class for Camel inspection.
 */
public abstract class AbstractCamelInspection extends LocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(AbstractCamelInspection.class);

    private boolean forceEnabled;

    protected AbstractCamelInspection() {
    }

    protected AbstractCamelInspection(boolean forceEnabled) {
        this.forceEnabled = forceEnabled;
    }

    boolean isInspectionEnabled(Project project) {
        return forceEnabled || project.getService(CamelService.class).isCamelProject();
    }

    /**
     * Override to provide special logic whether to accept the element.
     */
    boolean accept(PsiElement element) {
        return true;
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Inspects all Camel endpoints and languages";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
        if (isInspectionEnabled(holder.getProject())) {
            return new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (accept(element)) {
                        String text = IdeaUtils.getService().extractTextFromElement(element, false, false, true);
                        if (!StringUtils.isEmpty(text)) {
                            validateText(element, holder, text, isOnTheFly);
                        }
                    }
                }
            };
        } else {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
    }

    /**
     * Validate endpoint options list aka properties. eg "timer:trigger?delay=1000&bridgeErrorHandler=true"
     * if the URI is not valid a error annotation is created and highlight the invalid value.
     */
    private void validateText(@NotNull PsiElement element, final @NotNull ProblemsHolder holder, @NotNull String text, boolean isOnTheFly) {
        final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();
        if (!camelIdeaUtils.acceptForAnnotatorOrInspection(element)) {
            LOG.debug("Skipping complex element  " + element + " for inspecting text: " + text);
            return;
        }

        boolean hasSimple = text.contains("${") || text.contains("$simple{");
        if (hasSimple && camelIdeaUtils.isCamelExpression(element, "simple")) {
            validateSimple(element, holder, text, isOnTheFly);
        } else if (camelIdeaUtils.isCamelExpression(element, "jsonpath")) {
            validateJSonPath(element, holder, text, isOnTheFly);
        } else if (QueryUtils.isQueryContainingCamelComponent(element.getProject(), text)) {
            validateEndpoint(element, holder, text, isOnTheFly);
        }
    }

    private void validateSimple(@NotNull PsiElement element, final @NotNull ProblemsHolder holder, @NotNull String text, boolean isOnTheFly) {
        CamelCatalog catalogService = element.getProject().getService(CamelCatalogService.class).get();
        CamelService camelService = element.getProject().getService(CamelService.class);

        IElementType type = element.getNode().getElementType();
        LOG.trace("Element %s of type: %s to inspect simple: %s".formatted(element, type, text));

        try {
            // need to use the classloader that can load classes from the camel-core
            ClassLoader loader = camelService.getCamelCoreClassloader();
            if (loader != null) {
                LanguageValidationResult result;
                boolean predicate = CamelIdeaUtils.getService().isCamelExpressionUsedAsPredicate(element, "simple");
                if (predicate) {
                    LOG.debug("Inspecting simple predicate: " + text);
                    result = catalogService.validateLanguagePredicate(loader, "simple", text);
                } else {
                    LOG.debug("Inspecting simple expression: " + text);
                    result = catalogService.validateLanguageExpression(loader, "simple", text);
                }
                if (!result.isSuccess()) {
                    // favor the short error message
                    String msg = result.getShortError();
                    if (msg == null) {
                        msg = result.getError();
                    }
                    if ("[null]".equals(msg)) {
                        return;
                    }
                    holder.registerProblem(element, msg);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error inspection Camel simple: %s".formatted(text), e);
        }
    }

    private void validateJSonPath(@NotNull PsiElement element, final @NotNull ProblemsHolder holder, @NotNull String text, boolean isOnTheFly) {
        CamelCatalog catalogService = element.getProject().getService(CamelCatalogService.class).get();
        CamelService camelService = element.getProject().getService(CamelService.class);

        IElementType type = element.getNode().getElementType();
        LOG.trace("Element " + element + " of type: " + type + " to inspect jsonpath: " + text);

        // must have camel-json library
        boolean jsonLib = camelService.containsLibrary("camel-jsonpath", false);
        if (!jsonLib) {
            return;
        }

        try {
            // need to use the classloader that can load classes from the project
            ClassLoader loader = camelService.getProjectClassloader();
            if (loader != null) {
                LanguageValidationResult result;
                boolean predicate = CamelIdeaUtils.getService().isCamelExpressionUsedAsPredicate(element, "jsonpath");
                if (predicate) {
                    LOG.debug("Inspecting jsonpath predicate: " + text);
                    result = catalogService.validateLanguagePredicate(loader, "jsonpath", text);
                } else {
                    LOG.debug("Inspecting jsonpath expression: " + text);
                    result = catalogService.validateLanguageExpression(loader, "jsonpath", text);
                }
                if (!result.isSuccess()) {
                    // favor the short error message
                    String msg = result.getShortError();
                    if (msg == null) {
                        msg = result.getError();
                    }
                    if ("[null]".equals(msg)) {
                        return;
                    }
                    holder.registerProblem(element, msg);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error inspection Camel jsonpath: %s".formatted(text), e);
        }
    }

    private void validateEndpoint(@NotNull PsiElement element, final @NotNull ProblemsHolder holder, @NotNull String text, boolean isOnTheFly) {
        CamelCatalog catalogService = element.getProject().getService(CamelCatalogService.class).get();

        IElementType type = element.getNode().getElementType();
        LOG.trace("Element " + element + " of type: " + type + " to inspect endpoint uri: " + text);

        final CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();
        // skip special values such as configuring ActiveMQ brokerURL
        if (camelIdeaUtils.skipEndpointValidation(element)) {
            LOG.debug("Skipping element " + element + " for validation with text: " + text);
            return;
        }

        // camel catalog expects &amp; as & when it parses so replace all &amp; as &
        String camelQuery = text;
        camelQuery = camelQuery.replace("&amp;", "&");

        // strip up ending incomplete parameter
        if (camelQuery.endsWith("&") || camelQuery.endsWith("?")) {
            camelQuery = camelQuery.substring(0, camelQuery.length() - 1);
        }

        boolean stringFormat = camelIdeaUtils.isFromStringFormatEndpoint(element);
        if (stringFormat) {
            // if the node is fromF or toF, then replace all %X with {{%X}} as we cannot parse that value
            camelQuery = camelQuery.replaceAll("(%[bds])", "{{$1}}");
        }

        boolean consumerOnly = camelIdeaUtils.isConsumerEndpoint(element);
        boolean producerOnly = camelIdeaUtils.isProducerEndpoint(element);

        try {
            EndpointValidationResult result = catalogService.validateEndpointProperties(camelQuery, false, consumerOnly, producerOnly);

            extractMapValue(result, result.getInvalidBoolean(), text, element, holder, isOnTheFly, new AbstractCamelInspection.BooleanErrorMsg());
            extractMapValue(result, result.getInvalidEnum(), text, element, holder, isOnTheFly, new AbstractCamelInspection.EnumErrorMsg());
            extractMapValue(result, result.getInvalidInteger(), text, element, holder, isOnTheFly, new AbstractCamelInspection.IntegerErrorMsg());
            extractMapValue(result, result.getInvalidNumber(), text, element, holder, isOnTheFly, new AbstractCamelInspection.NumberErrorMsg());
            extractMapValue(result, result.getInvalidReference(), text, element, holder, isOnTheFly, new AbstractCamelInspection.ReferenceErrorMsg());
            extractSetValue(result, result.getUnknown(), text, element, holder, isOnTheFly, new AbstractCamelInspection.UnknownErrorMsg());
            extractSetValue(result, result.getNotConsumerOnly(), text, element, holder, isOnTheFly, new AbstractCamelInspection.NotConsumerOnlyErrorMsg());
            extractSetValue(result, result.getNotProducerOnly(), text, element, holder, isOnTheFly, new AbstractCamelInspection.NotProducerOnlyErrorMsg());
        } catch (Exception e) {
            LOG.warn("Error inspecting Camel endpoint: %s".formatted(text), e);
        }
    }

    private void extractSetValue(EndpointValidationResult result, Set<String> validationSet,
                                 String fromElement, PsiElement element, final @NotNull ProblemsHolder holder,
                                 boolean isOnTheFly, CamelAnnotatorEndpointMessage msg) {

        if (!result.isSuccess() && validationSet != null) {
            for (String entry : validationSet) {
                String desc = summaryErrorMessage(result, entry, msg);
                holder.registerProblem(element, desc);
            }
        }
    }

    private void extractMapValue(EndpointValidationResult result, Map<String, String> validationMap,
                                 String fromElement, @NotNull PsiElement element, final @NotNull ProblemsHolder holder,
                                 boolean isOnTheFly, CamelAnnotatorEndpointMessage msg) {

        if (!result.isSuccess() && validationMap != null) {
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
            boolean empty = entry.getValue() == null || entry.getValue().isEmpty();
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
            return property + " is unknown option";
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
     * A human-readable summary of the validation errors.
     *
     * @return the summary, or <tt>empty</tt> if no validation errors
     */
    private <T> String summaryErrorMessage(EndpointValidationResult result, T entry, CamelAnnotatorEndpointMessage<T> msg) {
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
