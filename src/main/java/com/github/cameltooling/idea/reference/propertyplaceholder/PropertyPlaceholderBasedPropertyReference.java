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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PropertyPlaceholderBasedPropertyReference extends PropertyReference {

    private final PropertyPlaceholderDefinition placeholderDefinition;

    public PropertyPlaceholderBasedPropertyReference(@NotNull PropertyPlaceholderDefinition placeholderDefinition,
                                                     @NotNull String key, @NotNull PsiElement element, @Nullable String bundleName, boolean soft, TextRange textRange) {
        super(key, element, bundleName, soft, textRange);
        this.placeholderDefinition = placeholderDefinition;
    }

    public PropertyPlaceholderDefinition getPlaceholderDefinition() {
        return placeholderDefinition;
    }

}
