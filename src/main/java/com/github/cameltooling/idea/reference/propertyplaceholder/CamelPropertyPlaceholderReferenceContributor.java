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
package com.github.cameltooling.idea.reference.propertyplaceholder;

import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;

import java.util.List;

import static com.github.cameltooling.idea.util.CamelIdeaUtils.*;

/**
 * Provides references for Camel property placeholders in the format {{propertyName}}, inside strings in:
 * <ul>
 *     <li>Java code string literals (e.g. in camel route endpoint uris)</li>
 *     <li>XML 'uri' attributes</li>
 * </ul>
 *
 * Using native IDEA {@link PropertyReference} references automatically enables renaming functionality.
 */
public class CamelPropertyPlaceholderReferenceContributor extends AbstractPropertyPlaceholderReferenceContributor<PropertyPlaceholderDefinition> {

    private static final PropertyPlaceholderDefinition DEF = new PropertyPlaceholderDefinition(PROPERTY_PLACEHOLDER_START_TOKEN, PROPERTY_PLACEHOLDER_END_TOKEN);

    @Override
    protected List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderLocations() {
        return getService().getAllowedPropertyPlaceholderLocations();
    }

    @Override
    protected List<PropertyPlaceholderDefinition> getPlaceholderDefinitions() {
        return List.of(DEF);
    }

}