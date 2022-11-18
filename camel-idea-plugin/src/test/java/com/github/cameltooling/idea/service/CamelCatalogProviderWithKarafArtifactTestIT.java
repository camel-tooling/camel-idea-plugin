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
package com.github.cameltooling.idea.service;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.jetbrains.annotations.Nullable;

/**
 * The integration tests ensuring that the {@link CamelCatalogProvider} works as expected in case the core artifact of
 * Camel Karaf is part of the dependencies of the project.
 */
public class CamelCatalogProviderWithKarafArtifactTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String CORE_RUNTIME_MAVEN_ARTIFACT = "org.apache.camel.karaf:camel-core-osgi:3.18.2";

    private final CamelCatalogProvider provider = CamelCatalogProvider.KARAF;

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CORE_RUNTIME_MAVEN_ARTIFACT};
    }

    /**
     * Ensure that the Catalog can be retrieved.
     */
    public void testGetCatalog() {
        CamelCatalog catalog = provider.get(getProject());
        assertNotNull(catalog.getRuntimeProvider());
        assertSame(catalog, catalog.getRuntimeProvider().getCamelCatalog());
        assertInstanceOf(catalog, DefaultCamelCatalog.class);
    }

    /**
     * Ensure that the runtime provider can be loaded.
     */
    public void testRuntimeProvider() {
        getProject().getService(CamelCatalogService.class).get().setVersionManager(new CamelMavenVersionManager());
        assertTrue(provider.loadRuntimeProviderVersion(getProject()));
    }
}
