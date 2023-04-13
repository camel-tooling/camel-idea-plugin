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
import com.intellij.codeInsight.lookup.LookupElement;
import org.hamcrest.Matchers;

import static org.junit.Assert.assertThat;

/**
 * Testing smart completion with Camel Java DSL
 */
public class JavaEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    protected void tearDown() throws Exception {
        try {
            CamelPreferenceService.getService().setOnlyShowKameletOptions(true);
        } finally {
            super.tearDown();
        }
    }

    public void testConsumerCompletion() {
        myFixture.configureByFiles("CompleteJavaEndpointConsumerTestData.java");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("file:inbox?fileExist", "file:inbox?forceWrites")));
        assertThat(strings, Matchers.hasItems("file:inbox?autoCreate", "file:inbox?include", "file:inbox?delay", "file:inbox?delete"));
        assertTrue("There are many options", strings.size() > 60);
    }

    public void testProducerCompletion() {
        myFixture.configureByFiles("CompleteJavaEndpointProducerTestData.java");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("file:outbox?autoCreate", "file:outbox?include", "file:outbox?delay", "file:outbox?delete")));
        assertThat(strings, Matchers.hasItems("file:outbox?fileExist", "file:outbox?forceWrites"));
        assertTrue("There is less options", strings.size() < 30);
    }

    private String getJavaInTheMiddleUnresolvedOptionsTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10&ex<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaInTheMiddleUnresolvedOptionsCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaInTheMiddleUnresolvedOptionsTestData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("timer:trigger?repeatCount=10")));
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&exceptionHandler",
            "timer:trigger?repeatCount=10&exchangePattern"));
        assertEquals("There is less options", 2, strings.size());
    }

    private String getJavaInTheMiddleOfResolvedOptionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10&fixed<caret>Rate=false\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }

    public void testJavaInTheMiddleOfResolvedOptionsCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaInTheMiddleOfResolvedOptionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("Expect 0 options", 0, strings.size());
    }

    private String getJavaAfterAmpOptionsTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10&<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaAfterAmpOptionsCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaAfterAmpOptionsTestData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "timer:trigger?repeatCount=10");
        assertContainsElements(strings, "timer:trigger?repeatCount=10&bridgeErrorHandler",
            "timer:trigger?repeatCount=10&daemon",
            "timer:trigger?repeatCount=10&delay",
            "timer:trigger?repeatCount=10&exceptionHandler",
            "timer:trigger?repeatCount=10&exchangePattern",
            "timer:trigger?repeatCount=10&fixedRate",
            "timer:trigger?repeatCount=10&pattern",
            "timer:trigger?repeatCount=10&period",
            "timer:trigger?repeatCount=10&synchronous",
            "timer:trigger?repeatCount=10&time",
            "timer:trigger?repeatCount=10&timer");
        assertTrue("There is less options", strings.size() < 13);
    }

    private String getJavaCaretAfterQuestionMarkWithPreDataOptionsTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?re<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaAfterQuestionMarkWithPreDataOptionsCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaCaretAfterQuestionMarkWithPreDataOptionsTestData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNull("Don't except any elements, because it the 're' is unique and return the repeatCount", strings);
    }

    private String getJavaEndOfLineTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=0&exchangePattern=RobustInOnly&<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaEndOfLineOptionsCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaEndOfLineTestData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 9);
    }



    private String getJavaInsertAfterQuestionMarkTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?per<caret>repeatCount=0&exchangePattern=RobustInOnly\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaInsertAfterQuestionMarkTestData() {
        String javaInsertAfterQuestionMarkTestData = getJavaInsertAfterQuestionMarkTestData();
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", javaInsertAfterQuestionMarkTestData);
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals("There are many options", 1, strings.size());
        assertThat(strings, Matchers.contains("timer:trigger?period"));
        myFixture.type('\n');
        javaInsertAfterQuestionMarkTestData = javaInsertAfterQuestionMarkTestData.replace("<caret>", "iod=");
        myFixture.checkResult(javaInsertAfterQuestionMarkTestData);
    }

    private String getJavaMultilineTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10&\" +\n"
            + "                \"fixedRate=false\"+ \n"
            + "                \"&daemon=false\" + \n"
            + "                \"&period=10<caret>\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaMultilineTestData() {
        myFixture.configureByText("CamelRoute.java", getJavaMultilineTestData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 1);
        assertThat(strings, Matchers.not(Matchers.containsInAnyOrder(
            "timer:trigger?repeatCount=10&",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String javaInsertAfterQuestionMarkTestData = getJavaMultilineTestData().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(javaInsertAfterQuestionMarkTestData);
    }

    private String getJavaMultilineTest2Data() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"\n"
            + "                + \"&fixedRate=false<caret>\"\n"
            + "                + \"&daemon=false\" \n"
            + "                + \"&period=10\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaMultilineTest2Data() {
        myFixture.configureByText("CamelRoute.java", getJavaMultilineTest2Data());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 1);
        assertThat(strings, Matchers.not(Matchers.containsInAnyOrder(
            "timer:trigger?repeatCount=10",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String javaInsertAfterQuestionMarkTestData = getJavaMultilineTest2Data().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(javaInsertAfterQuestionMarkTestData);
    }

    private String getJavaMultilineTest3Data() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"+\n"
            + "                 \"&fixedRate=false\"+\n"
            + "                 \"&daemon=false\" \n"
            + "                + \"&period=10&<caret>\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaMultilineTest3Data() {
        myFixture.configureByText("CamelRoute.java", getJavaMultilineTest3Data());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 1);
        assertThat(strings, Matchers.not(Matchers.containsInAnyOrder(
            "timer:trigger?repeatCount=10",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String javaInsertAfterQuestionMarkTestData = getJavaMultilineTest3Data().replace("<caret>", "bridgeErrorHandler=");
        myFixture.checkResult(javaInsertAfterQuestionMarkTestData);
    }

    private String getJavaMultilineInFixSearchData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"+\n"
            + "                 \"&ex<caret>fixedRate=false\"+\n"
            + "                 \"&daemon=false\" \n"
            + "                + \"&period=10\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaMultilineInFixSearchData() {
        myFixture.configureByText("CamelRoute.java", getJavaMultilineInFixSearchData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 1);
        assertThat(strings, Matchers.containsInAnyOrder("&exceptionHandler", "&exchangePattern"));
        assertThat(strings, Matchers.not(Matchers.containsInAnyOrder(
            "timer:trigger?repeatCount=10",
            "&fixedRate=false",
            "&daemon=false",
            "&period=10")));
        myFixture.type('\n');
        String javaInsertAfterQuestionMarkTestData = getJavaMultilineInFixSearchData().replace("<caret>", "ceptionHandler=");
        myFixture.checkResult(javaInsertAfterQuestionMarkTestData);
    }

    private String getJavaMultilineTest4SearchData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"+\n"
            + "                 \"&fixedRate=false\"+\n"
            + "                 \"&daemon=false&\" \n"
            + "                + \"period=10<caret>\");\n"
            + "        }\n"
            + "    }";
    }
    public void testJavaMultilineTest4Data() {
        myFixture.configureByText("CamelRoute.java", getJavaMultilineTest4SearchData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("There are many options", strings.size() > 1);
        assertThat(strings, Matchers.not(Matchers.containsInAnyOrder(
            "timer:trigger?repeatCount=10",
            "fixedRate=false",
            "daemon=false",
            "period=10")));
        myFixture.type('\n');
        String javaMarkTestData = getJavaMultilineTest4SearchData().replace("<caret>", "&bridgeErrorHandler=");
        myFixture.checkResult(javaMarkTestData);
    }

    static String getJavaSourceKameletSuggestionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"kamelet:<caret>\");\n"
            + "        }\n"
            + "    }";
    }

    /**
     * Ensure that the name of the available source Kamelets can be suggested
     */
    public void testJavaSourceKameletSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:avro-deserialize-action", "kamelet:aws-sqs-sink");
        assertContainsElements(strings, "kamelet:aws-s3-source", "kamelet:ftp-source", "kamelet:webhook-source");
        myFixture.type("ft\n");
        String javaMarkTestData = getJavaSourceKameletSuggestionsData().replace("<caret>", "ftp-source");
        myFixture.checkResult(javaMarkTestData);
    }

    private String getJavaNonSourceKameletSuggestionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"stream:in\")"
            + "                .to(\"kamelet:<caret>\");\n"
            + "        }\n"
            + "    }";
    }

    /**
     * Ensure that the name of the available non source Kamelets can be suggested
     */
    public void testJavaNonSourceKameletSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaNonSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:aws-s3-source", "kamelet:ftp-source", "kamelet:webhook-source");
        assertContainsElements(strings, "kamelet:avro-deserialize-action", "kamelet:aws-sqs-sink");
        myFixture.type("avro-d\n");
        String javaMarkTestData = getJavaNonSourceKameletSuggestionsData().replace("<caret>", "avro-deserialize-action");
        myFixture.checkResult(javaMarkTestData);
    }

    /**
     * Ensures that suggestions are only instances of {@link OptionSuggestion} when name of Kamelets are proposed.
     */
    public void testJavaKameletSuggestionsInstancesOfOptionSuggestion() {
        myFixture.configureByText("CamelRoute.java", getJavaSourceKameletSuggestionsData());
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions)
                .map(LookupElement::getObject)
                .anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    private String getJavaKameletOptionSuggestionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"kamelet:ftp-source?<caret>\");\n"
            + "        }\n"
            + "    }";
    }

    /**
     * Ensure that the configuration option of a given Kamelet can be suggested with other options.
     */
    public void testJavaKameletOptionSuggestions() {
        CamelPreferenceService.getService().setOnlyShowKameletOptions(false);
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "kamelet:ftp-source?connectionHost", "kamelet:ftp-source?connectionPort", "kamelet:ftp-source?bridgeErrorHandler");
        myFixture.type("user\n");
        String javaMarkTestData = getJavaKameletOptionSuggestionsData().replace("<caret>", "username=");
        myFixture.checkResult(javaMarkTestData);
    }

    /**
     * Ensure that the configuration option of a given Kamelet can be suggested without other options.
     */
    public void testJavaKameletOptionAloneSuggestions() {
        CamelPreferenceService.getService().setOnlyShowKameletOptions(true);
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:ftp-source?bridgeErrorHandler");
        assertContainsElements(strings, "kamelet:ftp-source?connectionHost", "kamelet:ftp-source?connectionPort");
        myFixture.type("connectionH\n");
        String javaMarkTestData = getJavaKameletOptionSuggestionsData().replace("<caret>", "connectionHost=");
        myFixture.checkResult(javaMarkTestData);
    }

    /**
     * Ensures that suggestions are only instances of {@link OptionSuggestion} when configuration options of a given
     * Kamelet are proposed.
     */
    public void testJavaKameletOptionSuggestionsInstancesOfOptionSuggestion() {
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionSuggestionsData());
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions)
                .map(LookupElement::getObject)
                .anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    private String getJavaKameletOptionValueSuggestionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"kamelet:ftp-source?passiveMode=<caret>\");\n"
            + "        }\n"
            + "    }";
    }

    /**
     * Ensure that the values of a configuration option of a given Kamelet can be suggested
     */
    public void testJavaKameletOptionValueSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionValueSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "kamelet:ftp-source?passiveMode=true", "kamelet:ftp-source?passiveMode=false");
        myFixture.type('\n');
        String javaMarkTestData = getJavaKameletOptionValueSuggestionsData().replace("<caret>", "false");
        myFixture.checkResult(javaMarkTestData);
    }

    /**
     * Ensures that suggestions are only instances of {@link OptionSuggestion} when the values of a configuration option
     * of a given Kamelet are proposed.
     */
    public void testJavaKameletOptionValueSuggestionsInstancesOfOptionSuggestion() {
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionValueSuggestionsData());
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions)
                .map(LookupElement::getObject)
                .anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    private String getJavaKameletOptionFilteredSuggestionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"kamelet:ftp-source?passiveMode=true&bridgeErrorHandler=true&<caret>\");\n"
            + "        }\n"
            + "    }";
    }

    /**
     * Ensure that configuration options of a given Kamelet can be filtered
     */
    public void testJavaKameletOptionFilteredSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionFilteredSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        String prefix = "kamelet:ftp-source?passiveMode=true&bridgeErrorHandler=true&";
        assertDoesntContain(strings, prefix + "passiveMode", prefix + "bridgeErrorHandler");
        assertContainsElements(strings, prefix + "connectionHost", prefix + "connectionPort");
        myFixture.type("user\n");
        String javaMarkTestData = getJavaKameletOptionFilteredSuggestionsData().replace("<caret>", "username=");
        myFixture.checkResult(javaMarkTestData);
    }
}
