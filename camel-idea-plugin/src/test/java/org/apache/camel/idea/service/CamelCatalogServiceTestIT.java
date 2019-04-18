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
package org.apache.camel.idea.service;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;


/**
 * Test the Camel Catalog is instantiated when camel is present
 */
public class CamelCatalogServiceTestIT extends CamelLightCodeInsightFixtureTestCaseIT {


    @Override
    protected void setUp() throws Exception {
        setIgnoreCamelCoreLib(true);
        super.setUp();
    }

    /*
      After upgrading to IDEA 2019.1 new constraints is added or changed on how test cases are behaving,
      and for some reason I suspect another test case does not clean up it's state and the CamelCatalogService is instantiated.
      Running the test case locally from IDEA and Maven works fine, but Travis CI fails the test.

      One of the things I observed was Travis runs test cases in a different order.
     */

//    public void testNoCatalogInstance() {
//        ServiceManager.getService(myModule.getProject(), CamelService.class).setCamelPresent(false);
//        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
//             "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
//        myFixture.complete(CompletionType.BASIC, 1);
//        assertEquals(false, ServiceManager.getService(myModule.getProject(), CamelCatalogService.class).isInstantiated());
//    }

    public void testCatalogInstance() {
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java", "CompleteYmlPropertyTestData.java",
            "CompleteJavaPropertyTestData.properties", "CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        assertEquals(true, ServiceManager.getService(myModule.getProject(), CamelCatalogService.class).isInstantiated());
    }

}
