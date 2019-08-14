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
package com.github.cameltooling.idea.service;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.components.ServiceManager;


/**
 * Test the Camel Catalog is instantiated when camel is present
 */
public class CamelCatalogServiceTestIT extends CamelLightCodeInsightFixtureTestCaseIT {


    @Override
    protected void setUp() throws Exception {
        setIgnoreCamelCoreLib(true);
        super.setUp();
    }

    public void testNoCatalogInstance() {
        ServiceManager.getService(getModule().getProject(), CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
            "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        assertEquals(false, ServiceManager.getService(getModule().getProject(), CamelCatalogService.class).isInstantiated());
    }

    public void testCatalogInstance() {
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
            "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        assertEquals(true, ServiceManager.getService(getModule().getProject(), CamelCatalogService.class).isInstantiated());
    }

}
