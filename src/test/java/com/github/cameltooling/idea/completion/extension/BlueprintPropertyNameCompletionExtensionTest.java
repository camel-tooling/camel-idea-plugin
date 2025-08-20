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

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;

import java.util.List;

public class BlueprintPropertyNameCompletionExtensionTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/propertyname";
    }

    public void testPropertyCompletionAtCorrectLocation() {
        List<String> strings = invokeCompletionWithFile("PersonBean.xml", "Person.java");

        assertNotNull(strings);
        assertEquals(3, strings.size());
        assertContainsElements(strings, "name", "age", "propertyWithSetterOnly");
    }


    public void testPropertyCompletionAtIncorrectLocation() {
        List<String> strings = invokeCompletionWithFile("PersonBeanWithWrongLocation.xml", "Person.java");

        assertEmpty(strings);
    }

    private List<String> invokeCompletionWithFile(String ... files) {
        myFixture.configureByFiles(files);
        myFixture.complete(CompletionType.BASIC);
        return myFixture.getLookupElementStrings();
    }

}