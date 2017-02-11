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
import org.hamcrest.Matchers;

import static org.junit.Assert.assertThat;

/**
 * Testing smart completion with Camel XML DSL
 */
public class XmlEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testConsumerCompletion() {
        myFixture.configureByFiles("CompleteXmlEndpointConsumerTestData.xml");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?autoCreate", "file:inbox?include", "file:inbox?delay", "file:inbox?delete")));
        assertFalse(strings.containsAll(Arrays.asList("file:inbox?fileExist", "file:inbox?forceWrites")));
        assertTrue("There is many options", strings.size() > 60);
    }

    public void testProducerCompletion() {
        myFixture.configureByFiles("CompleteXmlEndpointProducerTestData.xml");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertFalse(strings.containsAll(Arrays.asList("file:outbox?autoCreate", "file:outbox?include", "file:outbox?delay", "file:outbox?delete")));
        assertTrue(strings.containsAll(Arrays.asList("file:outbox?fileExist", "file:outbox?forceWrites")));
        assertTrue("There is less options", strings.size() < 30);
    }

    private String getXmlInsertAfterQuestionMarkTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?per<caret>repeatCount=0&amp;exchangePattern=RobustInOnly&amp;\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlInsertAfterQuestionMarkTestData() {
        String insertAfterQuestionMarkTestData = getXmlInsertAfterQuestionMarkTestData();
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", insertAfterQuestionMarkTestData);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is many options", strings.size() == 1);
        assertThat(strings, Matchers.contains("timer:trigger?period"));
        myFixture.type('\n');
        insertAfterQuestionMarkTestData = insertAfterQuestionMarkTestData.replace("<caret>", "iod=");
        myFixture.checkResult(insertAfterQuestionMarkTestData);
    }

    private String getXmlEndOfLineTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=0&amp;exchangePattern=RobustInOnly&amp;<caret>\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlEndOfLineOptionsCompletion() {
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", getXmlEndOfLineTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is many options", strings.size() > 9);
    }

    private String getXmlCaretAfterQuestionMarkWithPreDataOptionsTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?re<caret>\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlAfterQuestionMarkWithPreDataOptionsCompletion() {
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", getXmlCaretAfterQuestionMarkWithPreDataOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNull("Don't except any elements, because it the 're' is unique and return the repeatCount", strings);
    }

    private String getXmlfterAmpOptionsTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10&amp;<caret>\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlAfterAmbeCompletion() {
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", getXmlfterAmpOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("timer:trigger?repeatCount=10")));
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&amp;bridgeErrorHandler",
            "timer:trigger?repeatCount=10&amp;daemon",
            "timer:trigger?repeatCount=10&amp;delay",
            "timer:trigger?repeatCount=10&amp;exceptionHandler",
            "timer:trigger?repeatCount=10&amp;exchangePattern",
            "timer:trigger?repeatCount=10&amp;fixedRate",
            "timer:trigger?repeatCount=10&amp;pattern",
            "timer:trigger?repeatCount=10&amp;period",
            "timer:trigger?repeatCount=10&amp;synchronous",
            "timer:trigger?repeatCount=10&amp;time",
            "timer:trigger?repeatCount=10&amp;timer"));
        assertTrue("There is less options", strings.size() < 13);
    }

    private String getXmlInTheMiddleOfResolvedOptionsData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10&amp;fixed<caret>Rate=false\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlInTheMiddleOfResolvedOptionsCompletion() {
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", getXmlInTheMiddleOfResolvedOptionsData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("timer:trigger?repeatCount=10")));
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&amp;fixedRate"));
        assertTrue("There is less options", strings.size() == 1);
    }

    private String getXmlInTheMiddleUnresolvedOptionsTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10&amp;ex<caret>\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlInTheMiddleUnresolvedOptionsCompletion() {
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", getXmlInTheMiddleUnresolvedOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("timer:trigger?repeatCount=10")));
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&amp;exceptionHandler",
            "timer:trigger?repeatCount=10&amp;exchangePattern"));
        assertTrue("There is less options", strings.size() == 2);
        myFixture.type('\n');
        String result = getXmlInTheMiddleUnresolvedOptionsTestData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(result);
    }

    private String getXmlAfterValueWithOutAmpTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10<caret>\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlAfterValueWithOutAmpCompletion() {
        myFixture.configureByText("XmlAfterValueWithOutAmpCompletion.xml", getXmlAfterValueWithOutAmpTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is less options", strings.size() > 10);
        myFixture.type('\n');
        String result = getXmlAfterValueWithOutAmpTestData().replace("<caret>", "&amp;bridgeErrorHandler=");
        myFixture.checkResult(result);
    }
}