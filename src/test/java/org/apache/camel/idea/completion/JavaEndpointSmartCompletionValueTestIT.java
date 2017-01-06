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
package org.apache.camel.idea.completion;

import java.util.Arrays;
import java.util.List;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.apache.camel.idea.util.CamelService;

/**
 * Testing smart completion with Camel Java DSL
 */
public class JavaEndpointSmartCompletionValueTestIT extends LightCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ServiceManager.getService(myFixture.getProject(), CamelService.class).setCamelPresent(true);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }

    public void testEnumValue() {
        myFixture.configureByFiles("CompleteJavaEndpointValueEnumTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(8, strings.size());
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?readLock=changed", "file:inbox?readLock=fileLock", "file:inbox?readLock=idempotent",
            "file:inbox?readLock=idempotent-changed", "file:inbox?readLock=idempotent-rename", "file:inbox?readLock=markerFile",
            "file:inbox?readLock=none", "file:inbox?readLock=rename")));
    }

    public void testDefaultValue() {
        myFixture.configureByFiles("CompleteJavaEndpointValueDefaultTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(1, strings.size());
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?delay=500")));
    }

    public void testBooleanValue() {
        myFixture.configureByFiles("CompleteJavaEndpointValueBooleanTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(2, strings.size());
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?delay=1000&recursive=false", "file:inbox?delay=1000&recursive=true")));
    }

}