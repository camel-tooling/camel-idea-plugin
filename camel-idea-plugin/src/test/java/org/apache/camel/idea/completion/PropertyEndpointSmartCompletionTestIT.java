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
 * Testing smart completion with property files
 */
public class PropertyEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private String getInsertAfterQuestionMarkTestData() {
        return "TIMER=timer:trigger?per<caret>repeatCount=0&exchangePattern=RobustInOnly&";
    }

    public void testXmlInsertAfterQuestionMarkTestData() {
        String insertAfterQuestionMarkTestData = getInsertAfterQuestionMarkTestData();
        myFixture.configureByText("TestData.properties", insertAfterQuestionMarkTestData);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is many options", strings.size() == 1);
        assertThat(strings, Matchers.contains("timer:trigger?period"));
        myFixture.type('\n');
        insertAfterQuestionMarkTestData = insertAfterQuestionMarkTestData.replace("<caret>", "iod=");
        myFixture.checkResult(insertAfterQuestionMarkTestData);
    }

    private String getEndOfLineTestData() {
        return "TIMER=timer:trigger?repeatCount=0&exchangePattern=RobustInOnly&<caret>";
    }

    public void testEndOfLineOptionsCompletion() {
        myFixture.configureByText("TestData.properties", getEndOfLineTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is many options", strings.size() > 9);
    }

    private String getCaretAfterQuestionMarkWithPreDataOptionsTestData() {
        return "TIMER=timer:trigger?re<caret>";
    }

    public void testAfterQuestionMarkWithPreDataOptionsCompletion() {
        myFixture.configureByText("TestData.properties", getCaretAfterQuestionMarkWithPreDataOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNull("Don't except any elements, because it the 're' is unique and return the repeatCount", strings);
    }

    private String getfterAmpOptionsTestData() {
        return "TIME=timer:trigger?repeatCount=10&<caret>";
    }

    public void testAfterAmpCompletion() {
        myFixture.configureByText("TestData.properties", getfterAmpOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("timer:trigger?repeatCount=10")));
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&bridgeErrorHandler",
            "timer:trigger?repeatCount=10&daemon",
            "timer:trigger?repeatCount=10&delay",
            "timer:trigger?repeatCount=10&exceptionHandler",
            "timer:trigger?repeatCount=10&exchangePattern",
            "timer:trigger?repeatCount=10&fixedRate",
            "timer:trigger?repeatCount=10&pattern",
            "timer:trigger?repeatCount=10&period",
            "timer:trigger?repeatCount=10&synchronous",
            "timer:trigger?repeatCount=10&time",
            "timer:trigger?repeatCount=10&timer"));
        assertTrue("There is less options", strings.size() < 13);
    }

    private String getInTheMiddleOfResolvedOptionsData() {
        return "TIMER=timer:trigger?repeatCount=10&fixed<caret>Rate=false";
    }

    public void testInTheMiddleOfResolvedOptionsCompletion() {
        myFixture.configureByText("TestData.properties", getInTheMiddleOfResolvedOptionsData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("Expect 0 options", strings.size() == 0);
    }

    private String getInTheMiddleUnresolvedOptionsTestData() {
        return "TIMER=timer:trigger?repeatCount=10&ex<caret>";
    }

    public void testInTheMiddleUnresolvedOptionsCompletion() {
        myFixture.configureByText("TestData.properties", getInTheMiddleUnresolvedOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("timer:trigger?repeatCount=10")));
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&exceptionHandler",
            "timer:trigger?repeatCount=10&exchangePattern"));
        assertTrue("Expect exactly 2 options", strings.size() == 2);
        myFixture.type('\n');
        String result = getInTheMiddleUnresolvedOptionsTestData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(result);
    }

    private String getAfterValueWithOutAmpTestData() {
        return "TIMER=timer:trigger?repeatCount=10<caret>";
    }

    public void testAfterValueWithOutAmpCompletion() {
        myFixture.configureByText("TestData.properties", getAfterValueWithOutAmpTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is less options", strings.size() > 10);
        myFixture.type('\n');
        String result = getAfterValueWithOutAmpTestData().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

}
