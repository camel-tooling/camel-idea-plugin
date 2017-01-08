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
package org.apache.camel.idea;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelService;

/**
 * Super class for Camel Plugin Testing. If you are testing plug-in code with LightCodeInsightFixtureTestCase
 * you should extend this class to make sure it is setup as expected and clean up on tearDown
 */
public class CamelLightCodeInsightFixtureTestCaseIT extends LightCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        disposeOnTearDown(ServiceManager.getService(myModule.getProject(), CamelCatalogService.class));
        disposeOnTearDown(ServiceManager.getService(myModule.getProject(), CamelService.class));
        ServiceManager.getService(myModule.getProject(), CamelService.class).setCamelPresent(true);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }

    /**
     * To work around the problem with IDEA fail with "No tests found in org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT"
     */
    public void testDummy() {
        assertTrue(true);
    }
}
