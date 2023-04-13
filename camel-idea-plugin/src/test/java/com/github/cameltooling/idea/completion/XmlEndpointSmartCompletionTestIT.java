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
import com.github.cameltooling.idea.service.CamelPreferenceService;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Testing smart completion with Camel XML DSL
 */
public class XmlEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    protected void tearDown() throws Exception {
        try {
            CamelPreferenceService.getService().setOnlyShowKameletOptions(true);
        } finally {
            super.tearDown();
        }
    }

    public void testConsumerCompletion() {
        myFixture.configureByFiles("CompleteXmlEndpointConsumerTestData.xml");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue(strings.containsAll(Arrays.asList("file:inbox?autoCreate", "file:inbox?include", "file:inbox?delay", "file:inbox?delete")));
        assertFalse(strings.containsAll(Arrays.asList("file:inbox?fileExist", "file:inbox?forceWrites")));
        assertTrue("There are many options", strings.size() > 60);
    }

    public void testProducerCompletion() {
        myFixture.configureByFiles("CompleteXmlEndpointProducerTestData.xml");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, not(contains(Arrays.asList("file:outbox?autoCreate", "file:outbox?include", "file:outbox?delay", "file:outbox?delete"))));
        assertThat(strings, hasItems("file:outbox?fileExist", "file:outbox?forceWrites"));
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
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", insertAfterQuestionMarkTestData);
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 1, strings.size());
        assertThat(strings, contains("timer:trigger?period"));
        myFixture.type('\n');
        String result = insertAfterQuestionMarkTestData.replace("<caret>", "iod=");
        myFixture.checkResult(result);
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 9);
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNull("Don't except any elements, because it the 're' is unique and return the repeatCount", strings);
    }

    private String getXmlAfterAmpOptionsTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10&amp;<caret>\"/>\n"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    public void testXmlAfterAmpCompletion() {
        myFixture.configureByText("XmlCaretInMiddleOptionsTestData.xml", getXmlAfterAmpOptionsTestData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, not(contains("timer:trigger?repeatCount=10")));
        assertThat(strings, contains("&amp;bridgeErrorHandler",
            "&amp;daemon",
            "&amp;delay",
            "&amp;exceptionHandler",
            "&amp;exchangePattern",
            "&amp;fixedRate",
            "&amp;includeMetadata",
            "&amp;pattern",
            "&amp;period",
            "&amp;synchronous",
            "&amp;time",
            "&amp;timer"));
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There is less options", 0, strings.size());
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, not(contains("timer:trigger?repeatCount=10")));
        assertThat(strings, contains("&amp;exceptionHandler", "&amp;exchangePattern"));
        assertEquals("There is less options", 2, strings.size());
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
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There is less options", strings.size() > 10);
        myFixture.type('\n');
        String result = getXmlAfterValueWithOutAmpTestData().replace("<caret>", "&amp;bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getXmlMultilineTestData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10\n"
            + "         &amp;fixedRate=false\n"
            + "         &amp;daemon=false\n"
            + "         &amp;period=10<caret>\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }
    public void testXmlMultilineTestData() {
        myFixture.configureByText("CamelRoute.xml", getXmlMultilineTestData());
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
        String result = getXmlMultilineTestData().replace("<caret>", "&amp;bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getXmlMultilineTest2Data() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10\n"
            + "         &amp;fixedRate=false<caret>\n"
            + "         &amp;daemon=false\n"
            + "         &amp;period=10\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }
    public void testXmlMultilineTest2Data() {
        myFixture.configureByText("CamelRoute.xml", getXmlMultilineTest2Data());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 9, strings.size());
        assertThat(strings, not(containsInAnyOrder(
            "timer:trigger?repeatCount=10",
            "&amp;fixedRate=false",
            "&amp;daemon=false",
            "&amp;period=10")));
        myFixture.type('\n');
        String result = getXmlMultilineTest2Data().replace("<caret>", "&amp;bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getXmlMultilineTest3Data() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10\n"
            + "         &amp;fixedRate=false\n"
            + "         &amp;daemon=false\n"
            + "         &amp;period=10<caret>\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }
    public void testXmlMultilineTest3Data() {
        myFixture.configureByText("CamelRoute.xml", getXmlMultilineTest3Data());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 9, strings.size());
        assertThat(strings, not(containsInAnyOrder(
            "timer:trigger?repeatCount=10&amp;",
            "&amp;fixedRate=false",
            "&amp;daemon=false",
            "&amp;period=10")));
        myFixture.type('\n');
        String result = getXmlMultilineTest3Data().replace("<caret>", "&amp;bridgeErrorHandler=");
        myFixture.checkResult(result);
    }

    private String getXmlMultilineInFixSearchData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10\n"
            + "         &amp;ex<caret>fixedRate=false\n"
            + "         &amp;daemon=false\n"
            + "         &amp;period=10\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";

    }
    public void testXmlMultilineInFixSearchData() {
        myFixture.configureByText("CamelRoute.xml", getXmlMultilineInFixSearchData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 2, strings.size());
        assertThat(strings, containsInAnyOrder("&amp;exceptionHandler", "&amp;exchangePattern"));
        myFixture.type('\n');
        String result = getXmlMultilineInFixSearchData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(result);
    }

    private String getXmlSourceKameletSuggestionsData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"kamelet:<caret>\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    /**
     * Ensure that the name of the available source Kamelets can be suggested
     */
    public void testXmlSourceKameletSuggestions() {
        myFixture.configureByText("CamelRoute.xml", getXmlSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:avro-deserialize-action", "kamelet:aws-sqs-sink");
        assertContainsElements(strings, "kamelet:aws-s3-source", "kamelet:ftp-source", "kamelet:webhook-source");
        myFixture.type("ft\n");
        String javaMarkTestData = getXmlSourceKameletSuggestionsData().replace("<caret>", "ftp-source");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getXmlNonSourceKameletSuggestionsData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"stream:in\"/>"
            + "    <to uri=\"kamelet:<caret>\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    /**
     * Ensure that the name of the available non source Kamelets can be suggested
     */
    public void testXmlNonSourceKameletSuggestions() {
        myFixture.configureByText("CamelRoute.xml", getXmlNonSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:aws-s3-source", "kamelet:ftp-source", "kamelet:webhook-source");
        assertContainsElements(strings, "kamelet:avro-deserialize-action", "kamelet:aws-sqs-sink");
        myFixture.type("avro-d\n");
        String javaMarkTestData = getXmlNonSourceKameletSuggestionsData().replace("<caret>", "avro-deserialize-action");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getXmlKameletOptionSuggestionsData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"kamelet:ftp-source?<caret>\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    /**
     * Ensure that the configuration option of a given Kamelet can be suggested with other options.
     */
    public void testXmlKameletOptionSuggestions() {
        CamelPreferenceService.getService().setOnlyShowKameletOptions(false);
        myFixture.configureByText("CamelRoute.xml", getXmlKameletOptionSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "kamelet:ftp-source?connectionHost", "kamelet:ftp-source?connectionPort", "kamelet:ftp-source?bridgeErrorHandler");
        myFixture.type("user\n");
        String javaMarkTestData = getXmlKameletOptionSuggestionsData().replace("<caret>", "username=");
        myFixture.checkResult(javaMarkTestData);
    }

    /**
     * Ensure that the configuration option of a given Kamelet can be suggested without other options.
     */
    public void testXmlKameletOptionAloneSuggestions() {
        CamelPreferenceService.getService().setOnlyShowKameletOptions(true);
        myFixture.configureByText("CamelRoute.xml", getXmlKameletOptionSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:ftp-source?bridgeErrorHandler");
        assertContainsElements(strings, "kamelet:ftp-source?connectionHost", "kamelet:ftp-source?connectionPort");
        myFixture.type("connectionH\n");
        String javaMarkTestData = getXmlKameletOptionSuggestionsData().replace("<caret>", "connectionHost=");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getXmlKameletOptionValueSuggestionsData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"kamelet:ftp-source?passiveMode=<caret>\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    /**
     * Ensure that the values of a configuration option of a given Kamelet can be suggested
     */
    public void testXmlKameletOptionValueSuggestions() {
        myFixture.configureByText("CamelRoute.xml", getXmlKameletOptionValueSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "kamelet:ftp-source?passiveMode=true", "kamelet:ftp-source?passiveMode=false");
        myFixture.type('\n');
        String javaMarkTestData = getXmlKameletOptionValueSuggestionsData().replace("<caret>", "false");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getXmlKameletOptionFilteredSuggestionsData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"kamelet:ftp-source?passiveMode=true&amp;bridgeErrorHandler=true&amp;<caret>\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";
    }

    /**
     * Ensure that configuration options of a given Kamelet can be filtered
     */
    public void testXmlKameletOptionFilteredSuggestions() {
        myFixture.configureByText("CamelRoute.xml", getXmlKameletOptionFilteredSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        String prefix = "&amp;";
        assertDoesntContain(strings, prefix + "passiveMode", prefix + "bridgeErrorHandler");
        assertContainsElements(strings, prefix + "connectionHost", prefix + "connectionPort");
        myFixture.type("user\n");
        String javaMarkTestData = getXmlKameletOptionFilteredSuggestionsData().replace("<caret>", "username=");
        myFixture.checkResult(javaMarkTestData);
    }
}
