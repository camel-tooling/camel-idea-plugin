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
package com.github.cameltooling.idea.service.extension.camel;

import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class YamlCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    private static final List<String> YAML_ROUTES = Arrays.asList(
        new String[] {
            "routes",
            "routeConfigurations",
            "route",
            "routeConfiguration"
        });

    @Override
    public boolean isCamelFile(PsiFile file) {
        if (file != null && YAMLFileType.YML.equals(file.getFileType())) {
            YAMLFile yamlFile = (YAMLFile) file;
            List<YAMLDocument> yamlDocuments = yamlFile.getDocuments();
            return yamlDocuments.stream().anyMatch(document -> {
                YAMLValue value = document.getTopLevelValue();
                if (!(value instanceof YAMLSequence)) {
                    return false;
                }
                YAMLSequence sequence = (YAMLSequence) value;
                List<YAMLSequenceItem> sequenceItems = sequence.getItems();
                if (sequenceItems.isEmpty()) {
                    return false;
                }
                YAMLSequenceItem firstItem = sequenceItems.get(0);
                Collection<YAMLKeyValue> keysValues = firstItem.getKeysValues();
                if (keysValues.isEmpty()) {
                    return false;
                }
                YAMLKeyValue firstKeyValue = keysValues.iterator().next();
                if (firstKeyValue == null) {
                    return false;
                }
                return YAML_ROUTES.contains(firstKeyValue.getKeyText());
            });
        }

        return false;
    }

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        return false;
    }

    @Override
    public boolean isCamelRouteStartExpression(PsiElement element) {
        return false;
    }

    @Override
    public boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart) {
        return false;
    }

    @Override
    public boolean isCamelExpression(PsiElement element, String language) {
        return false;
    }

    @Override
    public boolean isCamelExpressionUsedAsPredicate(PsiElement element, String language) {
        return false;
    }

    @Override
    public boolean isConsumerEndpoint(PsiElement element) {
        return false;
    }

    @Override
    public boolean isProducerEndpoint(PsiElement element) {
        return false;
    }

    @Override
    public boolean skipEndpointValidation(PsiElement element) {
        return false;
    }

    @Override
    public boolean isFromStringFormatEndpoint(PsiElement element) {
        return false;
    }

    @Override
    public boolean acceptForAnnotatorOrInspection(PsiElement element) {
        return false;
    }

    @Override
    public boolean isExtensionEnabled() {
        final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.plugins.yaml"));
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public PsiClass getBeanClass(PsiElement element) {
        return null;
    }

    @Override
    public PsiElement getPsiElementForCamelBeanMethod(PsiElement element) {
        return null;
    }

    @Override
    public List<PsiElement> findEndpointUsages(Module module, Predicate<String> uriCondition) {
        return null;
    }

    @Override
    public List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition) {
        return null;
    }

    @Override
    public boolean isPlaceForEndpointUri(PsiElement location) {
        return false;
    }
}
