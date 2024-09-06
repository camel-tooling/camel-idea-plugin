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
import java.util.Optional;

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

import static com.github.cameltooling.idea.completion.endpoint.CamelSmartCompletionEndpointValue.addSmartCompletionForEndpointValue;

/**
 * {@code CamelKameletOptionValueCompletion} is responsible for completing the value of a Camel Kamelet option.
 */
public class CamelKameletOptionValueCompletion extends CamelKameletOptionCompletion {
    @Override
    protected @NotNull String getURIPrefix(PsiElement element) {
        String answer = "kamelet:";
        String name = getKameletName(element);
        if (name == null) {
            return answer;
        }
        answer += name + "?";
        String optionName = getOptionName(element);
        if (optionName == null) {
            return answer;
        }
        answer += optionName + "=";
        return answer;
    }

    protected @Nullable String getKameletName(PsiElement element) {
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        if (keyValue == null) {
            return null;
        } else if (!keyValue.getKeyText().equals("properties")) {
            keyValue = PsiTreeUtil.getParentOfType(keyValue, YAMLKeyValue.class);
            if (keyValue == null || !keyValue.getKeyText().equals("properties")) {
                return null;
            }
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

    /**
     * @return the name of the option or {@code null} if the element is not part of the configuration of Camel
     */
    private String getOptionName(PsiElement element) {
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        if (keyValue == null) {
            return null;
        }
        return keyValue.getKeyText();
    }

    @Override
    protected List<LookupElement> suggestions(@NotNull ComponentModel componentModel, @NotNull Project project,
                                              @NotNull PsiElement element, @NotNull Editor editor, String val) {
        String name = getOptionName(element);
        if (name == null) {
            return List.of();
        }
        ComponentModel.EndpointOptionModel endpointOption = componentModel.getEndpointOptions().stream().filter(
                o -> name.equals(o.getName()))
            .findFirst().orElse(null);
        if (endpointOption == null) {
            return List.of();
        }
        return addSmartCompletionForEndpointValue(editor, val, "", endpointOption, element, false);
    }
}
