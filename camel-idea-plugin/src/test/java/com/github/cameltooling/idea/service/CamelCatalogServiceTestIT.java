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
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.components.ServiceManager;
import org.apache.camel.catalog.CamelCatalog;


/**
 * Test the Camel Catalog is instantiated when camel is present
 */
public class CamelCatalogServiceTestIT extends CamelLightCodeInsightFixtureTestCaseIT {


    @Override
    protected void setUp() throws Exception {
        setIgnoreCamelCoreLib(true);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CamelPreferenceService.getService().setCamelCatalogProvider(null);
        } finally {
            super.tearDown();
        }
    }

    public void testNoCatalogInstance() {
        getModule().getProject().getService(CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
            "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        assertFalse(getModule().getProject().getService(CamelCatalogService.class).isInstantiated());
    }

    public void testCatalogInstance() {
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
            "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        assertTrue(getModule().getProject().getService(CamelCatalogService.class).isInstantiated());
    }

    /**
     * Ensure that the catalog is reloaded when the {@link CamelCatalogProvider} has been changed.
     */
    public void testCatalogChange() {
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
            "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        CamelCatalogService service = getModule().getProject().getService(CamelCatalogService.class);
        assertTrue(service.isInstantiated());
        CamelCatalog catalog = service.get();
        assertSame(catalog, service.get());
        CamelPreferenceService.getService().setCamelCatalogProvider(CamelCatalogProvider.QUARKUS);
        assertFalse(service.isInstantiated());
        assertNotSame(catalog, service.get());
    }

}
