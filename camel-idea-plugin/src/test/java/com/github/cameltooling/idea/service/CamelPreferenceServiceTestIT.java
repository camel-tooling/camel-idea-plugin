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

import java.util.concurrent.atomic.AtomicInteger;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.catalog.CamelCatalogProvider;

/**
 * Test class for {@link CamelPreferenceService}.
 */
public class CamelPreferenceServiceTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private CamelPreferenceService.CamelCatalogProviderChangeListener listener;

    @Override
    protected void tearDown() throws Exception {
        CamelPreferenceService.getService().setCamelCatalogProvider(null);
        if (listener != null) {
            CamelPreferenceService.getService().removeListener(listener);
        }
        super.tearDown();
    }

    /**
     * Ensure that the default {@link CamelCatalogProvider} is {@link CamelCatalogProvider#AUTO}
     */
    public void testAutoByDefault() {
        assertEquals(CamelCatalogProvider.AUTO, CamelPreferenceService.getService().getCamelCatalogProvider());
    }

    /**
     * Ensure that the listeners are notified when the {@link CamelCatalogProvider} has changed.
     */
    public void testChangeNotification() {
        AtomicInteger counter = new AtomicInteger();
        this.listener = counter::incrementAndGet;
        CamelPreferenceService service = CamelPreferenceService.getService();
        service.addListener(listener);
        assertEquals(0, counter.get());
        service.setCamelCatalogProvider(CamelCatalogProvider.AUTO);
        assertEquals(0, counter.get());
        service.setCamelCatalogProvider(null);
        assertEquals(0, counter.get());
        service.setCamelCatalogProvider(CamelCatalogProvider.SPRING_BOOT);
        assertEquals(1, counter.get());
        service.setCamelCatalogProvider(CamelCatalogProvider.SPRING_BOOT);
        assertEquals(1, counter.get());
        service.setCamelCatalogProvider(CamelCatalogProvider.DEFAULT);
        assertEquals(2, counter.get());
        service.setCamelCatalogProvider(null);
        assertEquals(3, counter.get());
    }
}
