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
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

/**
 * Testing smart completion with Camel Java DSL
 */
public class JavaEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testConsumerCompletion() {
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?autoCreate=", "file:inbox?include=", "file:inbox?delay=", "file:inbox?delete=")));
        assertFalse(strings.containsAll(Arrays.asList("file:inbox?fileExist=", "file:inbox?forceWrites=")));
        assertTrue("There is many options", strings.size() > 60);
    }

    public void testProducerCompletion() {
        myFixture.configureByFiles("CompleteJavaEndpointProducerTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertFalse(strings.containsAll(Arrays.asList("file:outbox?autoCreate=", "file:outbox?include=", "file:outbox?delay=", "file:outbox?delete=")));
        assertTrue(strings.containsAll(Arrays.asList("file:outbox?fileExist=", "file:outbox?forceWrites=")));
        assertTrue("There is less options", strings.size() < 30);
    }

}