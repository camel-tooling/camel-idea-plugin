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
package com.github.cameltooling.idea.completion;

import java.util.Arrays;
import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Testing smart completion with Camel YAML DSL
 */
public class YamlEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testConsumerCompletion() {
        myFixture.configureByFiles("CompleteYamlEndpointConsumerTestData.yaml");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?autoCreate", "file:inbox?include", "file:inbox?delay", "file:inbox?delete")));
        assertFalse(strings.containsAll(Arrays.asList("file:inbox?fileExist", "file:inbox?forceWrites")));
        assertTrue("There is many options", strings.size() > 60);
    }

    public void testProducerCompletion() {
        myFixture.configureByFiles("CompleteYamlEndpointProducerTestData.yaml");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, not(contains(Arrays.asList("file:outbox?autoCreate", "file:outbox?include", "file:outbox?delay", "file:outbox?delete"))));
        assertThat(strings, hasItems("file:outbox?fileExist", "file:outbox?forceWrites"));
        assertTrue("There is less options", strings.size() < 30);
    }

    private String getYamlInsertAfterQuestionMarkTestData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?per<caret>repeatCount=0&exchangePattern=RobustInOnly&\"\n"
            + "       steps:\n"
            + "         - to:\n"
            + "             uri: file:outbox?delete=true&fileExist=Append\n";
    }

    public void testYamlInsertAfterQuestionMarkTestData() {
        String insertAfterQuestionMarkTestData = getYamlInsertAfterQuestionMarkTestData();
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.yaml", insertAfterQuestionMarkTestData);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is many options", 1, strings.size());
        assertThat(strings, contains("timer:trigger?period"));
        myFixture.type('\n');
        String result = insertAfterQuestionMarkTestData.replace("<caret>", "iod=");
        myFixture.checkResult(result);
    }

    private String getYamlEndOfLineTestData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: timer:trigger?repeatCount=0&exchangePattern=RobustInOnly&<caret>\n"
            + "       steps:\n"
            + "         - to: file:outbox?delete=true&fileExist=Append\n";
    }

    public void testYamlEndOfLineOptionsCompletion() {
        myFixture.configureByText("YamlCaretInMiddleOptionsTestData.yaml", getYamlEndOfLineTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is many options", strings.size() > 9);
    }

    private String getYamlCaretAfterQuestionMarkWithPreDataOptionsTestData() {
        return "- route\n"
            + "    from:\n"
            + "      uri: timer:trigger?re<caret>\n"
            + "      steps:\n"
            + "        - to: file:outbox?delete=true&fileExist=Append\n";
    }

    public void testYamlAfterQuestionMarkWithPreDataOptionsCompletion() {
        myFixture.configureByText("YamlCaretInMiddleOptionsTestData.yaml", getYamlCaretAfterQuestionMarkWithPreDataOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNull("Don't except any elements, because it the 're' is unique and return the repeatCount", strings);
    }

    private String getYamlAfterAmpOptionsTestData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10&<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlAfterAmpCompletion() {
        myFixture.configureByText("YamlCaretInMiddleOptionsTestData.yaml", getYamlAfterAmpOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, not(contains("timer:trigger?repeatCount=10")));
        assertThat(strings, contains("&bridgeErrorHandler",
            "&daemon",
            "&delay",
            "&exceptionHandler",
            "&exchangePattern",
            "&fixedRate",
            "&includeMetadata",
            "&pattern",
            "&period",
            "&synchronous",
            "&time",
            "&timer"));
        assertTrue("There is less options", strings.size() < 13);
    }

    private String getYamlInTheMiddleOfResolvedOptionsData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10&fixed<caret>Rate=false\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlInTheMiddleOfResolvedOptionsCompletion() {
        myFixture.configureByText("YamlCaretInMiddleOptionsTestData.yaml", getYamlInTheMiddleOfResolvedOptionsData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is less options", 0, strings.size());
    }

    private String getYamlInTheMiddleUnresolvedOptionsTestData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10&ex<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlInTheMiddleUnresolvedOptionsCompletion() {
        myFixture.configureByText("YamlCaretInMiddleOptionsTestData.yaml", getYamlInTheMiddleUnresolvedOptionsTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, not(contains("timer:trigger?repeatCount=10")));
        assertThat(strings, contains("&exceptionHandler", "&exchangePattern"));
        assertEquals("There is less options", 2, strings.size());
        myFixture.type('\n');
        String result = getYamlInTheMiddleUnresolvedOptionsTestData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(result);
    }

    private String getYamlAfterValueWithOutAmpTestData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlAfterValueWithOutAmpCompletion() {
        myFixture.configureByText("YamlAfterValueWithOutAmpCompletion.yaml", getYamlAfterValueWithOutAmpTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue("There is less options", strings.size() > 10);
        myFixture.type('\n');
        String result = getYamlAfterValueWithOutAmpTestData().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getYamlMultilineTestData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10\n"
            + "         &fixedRate=false\n"
            + "         &daemon=false\n"
            + "         &period=10<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlMultilineTestData() {
        myFixture.configureByText("CamelRoute.yaml", getYamlMultilineTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is many options", 9, strings.size());
        assertThat(strings, not(containsInAnyOrder(
            "timer:trigger?repeatCount=10&",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String result = getYamlMultilineTestData().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getYamlMultilineTest2Data() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10\n"
            + "         &fixedRate=false<caret>\n"
            + "         &daemon=false\n"
            + "         &period=10\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlMultilineTest2Data() {
        myFixture.configureByText("CamelRoute.yaml", getYamlMultilineTest2Data());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is many options", 9, strings.size());
        assertThat(strings, not(containsInAnyOrder(
            "timer:trigger?repeatCount=10",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String result = getYamlMultilineTest2Data().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getYamlMultilineTest3Data() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10\n"
            + "         &fixedRate=false\n"
            + "         &daemon=false\n"
            + "         &period=10<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    public void testYamlMultilineTest3Data() {
        myFixture.configureByText("CamelRoute.yaml", getYamlMultilineTest3Data());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is many options", 9, strings.size());
        assertThat(strings, not(containsInAnyOrder(
            "timer:trigger?repeatCount=10&",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String result = getYamlMultilineTest3Data().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getYamlMultilineInFixSearchData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"timer:trigger?repeatCount=10\n"
            + "         &ex<caret>fixedRate=false\n"
            + "         &daemon=false\n"
            + "         &period=10\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";

    }

    public void testJavaMultilineInFixSearchData() {
        myFixture.configureByText("CamelRoute.yaml", getYamlMultilineInFixSearchData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is many options", 2, strings.size());
        assertThat(strings, containsInAnyOrder("&exceptionHandler", "&exchangePattern"));
        myFixture.type('\n');
        String result = getYamlMultilineInFixSearchData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(result);
    }
}
