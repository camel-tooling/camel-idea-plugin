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
package org.apache.camel.idea.facet;

import javax.swing.*;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelFacetType extends FacetType<CamelFacet, CamelFacetConfiguration> {
    private static final FacetTypeId<CamelFacet> TYPE_ID = new FacetTypeId<CamelFacet>(CamelFacet.ID);

    public CamelFacetType() {
        super(TYPE_ID, CamelFacet.ID, "Apache Camel");
    }

    @Override
    public CamelFacetConfiguration createDefaultConfiguration() {
        return new CamelFacetConfiguration();
    }

    @Override
    public CamelFacet createFacet(@NotNull Module module, String s, @NotNull CamelFacetConfiguration camelFacetConfiguration, @Nullable Facet facet) {
        return new CamelFacet(this, module, s, camelFacetConfiguration, facet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return moduleType instanceof JavaModuleType;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/camel.png");
    }

    public static CamelFacetType getInstance() {
        return findInstance(CamelFacetType.class);
    }
}
