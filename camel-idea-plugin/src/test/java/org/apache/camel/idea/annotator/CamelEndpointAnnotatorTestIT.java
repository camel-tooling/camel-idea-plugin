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
package org.apache.camel.idea.annotator;

import java.util.List;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

/**
 * Test Camel URI validation and the expected value is highlighted
 * TODO : Still need to find out how we can make a positive test without it complaining about it can't find SDK classes
 *
 * TIP : Writing highlighting test can be tricky because if the highlight is one character off
 * it will fail, but the error messaged might still be correct. In this case it's likely the TextRange
 * is incorrect.
 */
public class CamelEndpointAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator/";
    }

    public void testAnnotatorStringFormatValid() {
        myFixture.configureByText("AnnotatorTestData.java", getStringFormatValidEndpoint());
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();

        // there should not be any invalid boolean warnings as String.format should work
        boolean found = list.stream().anyMatch(i -> i.getDescription() != null && i.getDescription().startsWith("Invalid boolean value"));
        assertFalse("Should not find any warning", found);
    }

    public void testAnnotatorInvalidBooleanPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidBooleanPropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorInvalidBooleanPropertyProducerValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidBooleanPropertyInProducerTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorInvalidBooleanPropertyProducerOpenUriValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidBooleanPropertyInProducerWithOpenUriTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorUnknownOptionValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaUnknownOptionsConsumerTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorWithTheSameWordTwiceValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithSameWordTwiceTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorUnknownOptionWithPAthValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaUnknownOptionsWithPathConsumerTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorUnknownOptionWithConsumerAnnotationValidation() {
        assertTrue("Ignored until we fix the issue with running the test with SDK", true);
        //myFixture.configureByText("AnnotatorTestData.java", getJavaUnknownOptionsConsumerAnnotationTestData());
        //myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorInvalidBooleanPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlInvalidBooleanPropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlMultipleErrorsValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlMultipleErrorsTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorIntegerPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidIntegerPropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorIntegerPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlInvalidIntegerPropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorWithCommentValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlWithCommentTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorReferencePropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidReferencePropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorReferencePropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlInvalidReferencePropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testPropertyAnnotatorReferencePropertyValidation() {
        myFixture.configureByText("my_property_file.properties", getPropertyInvalidReferencePropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testJavaMultilineTest1SearchDataValidation() {
        myFixture.configureByText("myjavacode.java", getJavaMultilineTestValidationData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testJavaMultilineTest2SearchDataValidation() {
        myFixture.configureByText("myjavacode.java", getJavaMultilineTest2ValidationData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testJavaMultilineDoubleQuoteEndingStringTestValidationData() {
        myFixture.configureByText("myjavacode.java", getJavaMultilineDoubleQuoteEndingStringTestValidationData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlMultilineTestSearchDataValidation() {
        myFixture.configureByText("myxmlcode.xml", getXmlMultilineTest1ValidationData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlMultilineTest2SearchDataValidation() {
        myFixture.configureByText("myxmlcode.xml", getXmlMultilineTest2ValidationData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorConsumerOnly() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaConsumerOnlyTestData());
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();

        // find the warning from the highlights as checkWarning cannot do that for us for warnings
        boolean found = list.stream().anyMatch(i -> i.getText().equals("fileExist")
            && i.getDescription().equals("Option not applicable in consumer only mode")
            && i.getSeverity().equals(HighlightSeverity.WARNING));
        assertTrue("Should find the warning", found);
    }

    public void testAnnotatorProducerOnly() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaProducerOnlyTestData());
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();

        // find the warning from the highlights as checkWarning cannot do that for us for warnings
        boolean found = list.stream().anyMatch(i -> i.getText().equals("delete")
            && i.getDescription().equals("Option not applicable in producer only mode")
            && i.getSeverity().equals(HighlightSeverity.WARNING));
        assertTrue("Should find the warning", found);
    }

    public void testAnnotatorFromF() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaFromFTestData());
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();

        // there should not be any invalid boolean warnings as fromF should work
        boolean found = list.stream().anyMatch(i -> i.getDescription() != null && i.getDescription().startsWith("Invalid boolean value"));
        assertFalse("Should not find any warning", found);
    }

    private String getStringFormatValidEndpoint() {
        return "public class MyDummy {\n"
            + "    public String myUrl() {\n"
            + "        return String.format(\"seda:foo?blockWhenFull=%s\", true);\n"
            + "    }\n"
            + "}\n";
    }

    private String getJavaInvalidBooleanPropertyTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaInvalidBooleanPropertyInProducerTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=false\")\n"
            + "                .to(\"file:test?allowNullBody=<error descr=\"Invalid boolean value: FISH\">FISH</error>\")\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaUnknownOptionsConsumerTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=false\")\n"
            + "                .to(\"file:test?allowNullBody=true&<error descr=\"Unknown option\">foo</error>=bar\")\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaWithSameWordTwiceTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?delay=<error descr=\"Invalid integer value: foo\">foo</error>&<error descr=\"Unknown option\">foo</error>=bar\")\n"
            + "                .to(\"file:test?allowNullBody=true&<error descr=\"Unknown option\">foo</error>=bar\")\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaUnknownOptionsWithPathConsumerTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=false\")\n"
            + "                .to(\"file:foo?allowNullBody=true&<error descr=\"Unknown option\">foo</error>=bar\")\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaUnknownOptionsConsumerAnnotationTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "         @Consumer(file:test?allowNullBody=true&<error descr=\"Unknown option\">foo</error>=bar);"
            + "         public void onCheese(String name) {}"
            + "    }";
    }

    private String getJavaInvalidBooleanPropertyInProducerWithOpenUriTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=false\")\n"
            + "                .to(\"file:test?allowNullBody=<error descr=\"Invalid boolean value: FISH\">FISH</error>\")\n"
            + "        }\n"
            + "    }";
    }

    private String getXmlInvalidBooleanPropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    private String getXmlMultipleErrorsTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>&amp;<error descr=\"Unknown option\">foo</error>=bar\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    private String getXmlWithCommentTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "   <!-- Testing with comments inside xml -->\n"
            + "      <from uri=\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    private String getJavaInvalidIntegerPropertyTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?delay=<error descr=\"Invalid integer value: ImNotANumber\">ImNotANumber</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getXmlInvalidIntegerPropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"timer:trigger?delay=<error descr=\"Invalid integer value: ImNotANumber\">ImNotANumber</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    private String getJavaInvalidReferencePropertyTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"jms:queue:myqueue?jmsKeyFormatStrategy=<error descr=\"Invalid enum value: foo. Possible values: "
            + "[default, passthrough]. Did you mean: [default, passthrough]\">foo</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getXmlInvalidReferencePropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"jms:queue:myqueue?jmsKeyFormatStrategy=<error descr=\"Invalid enum value: foo. Possible values: "
            + "[default, passthrough]. Did you mean: [default, passthrough]\">foo</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    private String getPropertyInvalidReferencePropertyTestData() {
        return "my.jms = jms:queue:myqueue?jmsKeyFormatStrategy=<error descr=\"Invalid enum value: foo. Possible values: [default, passthrough]. Did you mean: [default, passthrough]\">foo</error>";
    }

    private String getJavaConsumerOnlyTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"file:inbox?delete=true&fileExist=Append\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaProducerOnlyTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"file:inbox?delete=true\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaFromFTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            fromF(\"file:inbox?delete=%s\", true)\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaMultilineTestValidationData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"+\n"
            + "                 \"&fixedRate=false\"+\n"
            + "                 \"&daemon=false&\" \n"
            + "                + \"<error descr=\"Unknown option\">pexriod</error>=10\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaMultilineTest2ValidationData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"+\n"
            + "                 \"&fixedRate=<error descr=\"Invalid boolean value: falxse\">falxse</error>\"+\n"
            + "                 \"&daemon=false&\" \n"
            + "                + \"<error descr=\"Unknown option\">pexriod</error>=10\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaMultilineDoubleQuoteEndingStringTestValidationData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10\"+\n"
            + "                 \"&fixedRate=<error descr=\"Invalid boolean value: falxse\">falxse</error>\"+\n"
            + "                 \"&daemon=false&\" \n"
            + "                + \"<error descr=\"Unknown option\">pexriod</error>=10\"\");\n"
            + "        }\n"
            + "    }";
    }

    private String getXmlMultilineTest1ValidationData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10\n"
            + "         &amp;ex<caret>fixedRate=false\n"
            + "         &amp;daemon=false\n"
            + "         &amp;<error descr=\"Unknown option\">pexriod</error>=10\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";

    }
    private String getXmlMultilineTest2ValidationData() {
        return "<routes>\n"
            + "  <route>\n"
            + "    <from uri=\"timer:trigger?repeatCount=10\n"
            + "         &amp;ex<caret>fixedRate=false\n"
            + "         &amp;daemon=<error descr=\"Invalid boolean value: falxse\">falxse</error>\n"
            + "         &amp;<error descr=\"Unknown option\">pexriod</error>=10\"/>"
            + "    <to uri=\"file:outbox?delete=true&amp;fileExist=Append\"/>\n"
            + "  </route>\n"
            + "</routes>";

    }


}