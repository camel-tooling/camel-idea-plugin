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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?autoCreate", "file:inbox?include", "file:inbox?delay", "file:inbox?delete")));
        assertFalse(strings.containsAll(Arrays.asList("file:inbox?fileExist", "file:inbox?forceWrites")));
        assertTrue("There are many options", strings.size() > 60);
    }

    public void testProducerCompletion() {
        myFixture.configureByFiles("CompleteYamlEndpointProducerTestData.yaml");
        myFixture.completeBasic();
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
        myFixture.configureByText("YamlCaretInMiddleOptionsTestData.yaml", insertAfterQuestionMarkTestData);
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 1, strings.size());
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 9);
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
        myFixture.completeBasic();
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
        myFixture.completeBasic();
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
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
        myFixture.completeBasic();
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 9, strings.size());
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 9, strings.size());
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 9, strings.size());
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

    public void testYamlMultilineInFixSearchData() {
        myFixture.configureByText("CamelRoute.yaml", getYamlMultilineInFixSearchData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 2, strings.size());
        assertThat(strings, containsInAnyOrder("&exceptionHandler", "&exchangePattern"));
        myFixture.type('\n');
        String result = getYamlMultilineInFixSearchData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(result);
    }

    private String getYamlKameletSuggestionsData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"kamelet:<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    /**
     * Ensure that the name of the available Kamelets can be suggested
     */
    public void testYamlKameletSuggestions() {
        myFixture.configureByText("CamelRoute.yaml", getYamlKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:avro-deserialize-action", "kamelet:aws-sqs-sink");
        assertContainsElements(strings, "kamelet:aws-s3-source", "kamelet:ftp-source", "kamelet:webhook-source");
        myFixture.type("ft\n");
        String javaMarkTestData = getYamlKameletSuggestionsData().replace("<caret>", "ftp-source");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getYamlNonSourceKameletSuggestionsData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"stream:in\"\n"
            + "       steps:\n"
            + "         - to: \"kamelet:<caret>\"\n";
    }

    /**
     * Ensure that the name of the available non source Kamelets can be suggested
     */
    public void testYamlNonSourceKameletSuggestions() {
        myFixture.configureByText("CamelRoute.yaml", getYamlNonSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:aws-s3-source", "kamelet:ftp-source", "kamelet:webhook-source");
        assertContainsElements(strings, "kamelet:avro-deserialize-action", "kamelet:aws-sqs-sink");
        myFixture.type("avro-d\n");
        String javaMarkTestData = getYamlNonSourceKameletSuggestionsData().replace("<caret>", "avro-deserialize-action");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getYamlKameletOptionSuggestionsData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"kamelet:ftp-source?<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    /**
     * Ensure that the configuration option of a given Kamelet can be suggested
     */
    public void testYamlKameletOptionSuggestions() {
        myFixture.configureByText("CamelRoute.yaml", getYamlKameletOptionSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "kamelet:ftp-source?connectionHost", "kamelet:ftp-source?connectionPort", "kamelet:ftp-source?bridgeErrorHandler");
        myFixture.type("user\n");
        String javaMarkTestData = getYamlKameletOptionSuggestionsData().replace("<caret>", "username=");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getYamlKameletOptionValueSuggestionsData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"kamelet:ftp-source?passiveMode=<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    /**
     * Ensure that the values of a configuration option of a given Kamelet can be suggested
     */
    public void testYamlKameletOptionValueSuggestions() {
        myFixture.configureByText("CamelRoute.yaml", getYamlKameletOptionValueSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "kamelet:ftp-source?passiveMode=true", "kamelet:ftp-source?passiveMode=false");
        myFixture.type('\n');
        String javaMarkTestData = getYamlKameletOptionValueSuggestionsData().replace("<caret>", "false");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getYamlKameletOptionFilteredSuggestionsData() {
        return "- route\n"
            + "     from:\n"
            + "       uri: \"kamelet:ftp-source?passiveMode=true&bridgeErrorHandler=true&<caret>\"\n"
            + "       steps:\n"
            + "         - to: \"file:outbox?delete=true&fileExist=Append\"\n";
    }

    /**
     * Ensure that configuration options of a given Kamelet can be filtered
     */
    public void testYamlKameletOptionFilteredSuggestions() {
        myFixture.configureByText("CamelRoute.yaml", getYamlKameletOptionFilteredSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        String prefix = "&";
        assertDoesntContain(strings, prefix + "passiveMode", prefix + "bridgeErrorHandler");
        assertContainsElements(strings, prefix + "connectionHost", prefix + "connectionPort");
        myFixture.type("user\n");
        String javaMarkTestData = getYamlKameletOptionFilteredSuggestionsData().replace("<caret>", "username=");
        myFixture.checkResult(javaMarkTestData);
    }
}
