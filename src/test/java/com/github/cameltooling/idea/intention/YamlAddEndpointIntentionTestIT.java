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
package com.github.cameltooling.idea.intention;

import java.util.List;
import java.util.Optional;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.intention.IntentionAction;

/**
 * Testing add Camel endpoint intention with Camel YAML DSL
 */
public class YamlAddEndpointIntentionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testAddCamelEndpoint() {
        myFixture.configureByFiles("YamlAddEndpointIntentionTestData.yaml");
        List<IntentionAction> intentions = myFixture.getAvailableIntentions();
        assertNotNull(intentions);

        // should be Camel in there
        Optional<IntentionAction> oa = intentions.stream().filter(a -> a.getText().equals("Add camel endpoint")).findFirst();
        assertTrue(oa.isPresent());

        // we cannot further test as the intention uses a popup to show a selection list for the user
    }

}
