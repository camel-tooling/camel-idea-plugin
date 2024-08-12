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
import com.github.cameltooling.idea.maven.CamelMavenVersionManager;
import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.testFramework.ServiceContainerUtil;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;

/**
 * The integration tests ensuring that the {@link CamelCatalogProvider} works as expected in case the core artifact of
 * a Camel Runtime is not part of the dependencies of the project.
 */
public class CamelCatalogProviderWithoutRuntimeArtifactTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ServiceContainerUtil.registerServiceInstance(getProject(), CamelService.class, new CamelService(getProject()) {
            @Override
            public ArtifactCoordinates getProjectCamelCoreCoordinates() {
                return ArtifactCoordinates.of("org.apache.camel", "camel-core-engine", "3.18.1");
            }
        });
    }

    /**
     * Ensure that the Karaf Catalog can be retrieved.
     */
    public void testGetKarafCatalog() {
        testGetCatalog(CamelCatalogProvider.KARAF);
    }

    /**
     * Ensure that the Spring Boot Catalog can be retrieved.
     */
    public void testGetSpringBootCatalog() {
        testGetCatalog(CamelCatalogProvider.SPRING_BOOT);
    }

    private void testGetCatalog(CamelCatalogProvider provider) {
        CamelCatalog catalog = provider.get(getProject());
        assertNotNull(catalog.getRuntimeProvider());
        assertSame(catalog, catalog.getRuntimeProvider().getCamelCatalog());
        assertInstanceOf(catalog, DefaultCamelCatalog.class);
    }

    /**
     * Ensure that the Karaf runtime provider can be loaded.
     */
    public void testKarafRuntimeProvider() {
        testRuntimeProvider(CamelCatalogProvider.KARAF);
    }

    /**
     * Ensure that the SpringBoot runtime provider can be loaded.
     */
    public void testSpringBootRuntimeProvider() {
        testRuntimeProvider(CamelCatalogProvider.SPRING_BOOT);
    }

    private void testRuntimeProvider(CamelCatalogProvider provider) {
        getProject().getService(CamelCatalogService.class).get().setVersionManager(new CamelMavenVersionManager(getProject()));
        assertTrue(provider.loadRuntimeProviderVersion(getProject()));
    }
}
