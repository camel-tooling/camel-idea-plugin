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
package com.github.cameltooling.idea.completion.extension;

import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.jetbrains.annotations.Nullable;

/**
 * Testing camel endpoint name completion in Java and XML DSL
 */
public class CamelEndpointNameCompletionExtensionIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/endpointname";
    }

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MODEL_MAVEN_ARTIFACT};
    }

    public void testDirectEndpointNameCompletionInJava() {
        myFixture.configureByFiles("CompleteDirectEndpointNameTestData.java");
        doTestCompletion();
    }

    public void testDirectEndpointNameCompletionInXml() {
        myFixture.configureByFiles("CompleteDirectEndpointNameTestData.xml");
        myFixture.completeBasic();
        doTestCompletion();
    }

    public void testDirectEndpointNameCompletionInYaml() {
        myFixture.configureByFiles("CompleteDirectEndpointNameTestData.yaml");
        myFixture.completeBasic();
        doTestCompletion();
    }

    public void testDirectEndpointNameCompletionInJavaAtInvalidPlace() {
        myFixture.configureByFiles("CompleteDirectEndpointNameAtInvalidPlace.java");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInXmlAtInvalidPlace() {
        myFixture.configureByFiles("CompleteDirectEndpointNameAtInvalidPlace.xml");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInYamlAtInvalidPlace() {
        myFixture.configureByFiles("CompleteDirectEndpointNameAtInvalidPlace.yaml");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInNonUriAttribute() {
        myFixture.configureByFiles("CompleteDirectEndpointNameInNonUriAttribute.xml");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInXmlInRouteStart() {
        myFixture.configureByFiles("CompleteDirectEndpointNameInRouteStart.xml");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInYamlInRouteStart() {
        myFixture.configureByFiles("CompleteDirectEndpointNameInRouteStart.yaml");
        doTestCompletionAtInvalidPlace();
    }

    private void doTestCompletion() {
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "direct:abc", "direct:def", "direct:test");
    }

    private void doTestCompletionAtInvalidPlace() {
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        if (strings != null) {
            assertDoesntContain(strings, "direct:abc", "direct:def", "direct:test", "file:inbox");
        }
    }
}
