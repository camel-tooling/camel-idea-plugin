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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointOptions;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;

import static com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestionsQueryParameters;

/**
 * {@code CamelKameletOptionNameCompletion} is responsible for completing the name of a Camel Kamelet option.
 */
public class CamelKameletOptionNameCompletion extends CamelKameletOptionCompletion {

    @Override
    protected List<LookupElement> suggestions(@NotNull ComponentModel componentModel, @NotNull Project project,
                                              @NotNull PsiElement element, @NotNull Editor editor, String val) {
        boolean consumerOnly = isConsumerOnly(project, element);
        return addSmartCompletionSuggestionsQueryParameters(
            new Context(val, consumerOnly), componentModel, existing(element), element, editor
        );
    }

    /**
     * @return a map of already configured options if any, an empty map otherwise
     */
    private Map<String, String> existing(PsiElement element) {
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        if (keyValue == null || !keyValue.getKeyText().equals("properties")) {
            return Map.of();
        }
        PsiElement lastChild = keyValue.getLastChild();
        if (lastChild instanceof YAMLMapping mapping) {
            return mapping.getKeyValues().stream().collect(Collectors.toMap(
                YAMLKeyValue::getKeyText, YAMLKeyValue::getValueText
            ));
        }
        return Map.of();
    }

    protected @Nullable String getKameletName(PsiElement element) {
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        if (keyValue == null || !keyValue.getKeyText().equals("properties")) {
            return null;
        }
        YAMLMapping mapping = PsiTreeUtil.getParentOfType(keyValue, YAMLMapping.class);
        if (mapping == null) {
            return null;
        }
        YAMLKeyValue ref = mapping.getKeyValueByKey("ref");
        if (ref == null) {
            return null;
        }
        YAMLValue value = ref.getValue();
        if (value instanceof YAMLMapping map) {
            return Optional.ofNullable(map.getKeyValueByKey("name"))
                .map(YAMLKeyValue::getValueText)
                .orElse(null);
        }
        return null;
    }

    @Override
    protected @NotNull String getURIPrefix(PsiElement element) {
        String name = getKameletName(element);
        if (name == null) {
            return "kamelet:";
        }
        return "kamelet:" + name + "?";
    }

    /**
     * {@code Context} is a context for the smart completion of Camel Kamelet option names.
     */
    private static class Context implements CamelSmartCompletionEndpointOptions.SmartCompletionSuggestionsQueryParametersContext {

        private final String val;
        private final boolean consumerOnly;

        Context(String val, boolean consumerOnly) {
            this.val = val;
            this.consumerOnly = consumerOnly;
        }

        @Override
        public String getQueryAtPosition() {
            return val;
        }

        @Override
        public String getConcatQuery() {
            return null;
        }

        @Override
        public String getSuffix() {
            return val;
        }

        @Override
        public boolean isXmlMode() {
            return false;
        }

        @Override
        public boolean isLookupAsURI() {
            return false;
        }

        @Override
        public boolean isConsumerOnly() {
            return consumerOnly;
        }

        @Override
        public boolean isProducerOnly() {
            return !consumerOnly;
        }

        @Override
        public char getLookupSuffixChar() {
            return ':';
        }

        @Override
        public String getLookupSuffix() {
            return ": ";
        }
    }
}
