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

import java.util.List;
import com.intellij.codeInsight.completion.CompletionType;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;

/**
 * Testing smart completion with Camel Java DSL
 */
public class JavaEndpointSmartCompletionMultiValueTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testMultiValueCompletion() {
        myFixture.configureByFiles("CompleteJavaEndpointMultiValueTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        // there should be plenty of options
        assertTrue(strings.size() > 60);

        // should include the scheduler. prefix again because its a multi value
        assertThat(strings, Matchers.hasItems("file:inbox?scheduler=quartz2&scheduler.foo=123&scheduler."));
    }

}