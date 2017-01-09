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

import com.intellij.openapi.fileTypes.FileType;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

/**
 * Test Camel URI validation and the expected value is highlighted
 * TODO : Still need to find out how we can make a positive test without it complaining about it can't find SDK classes
 *
 * TIP : Writing highlighting test can be tricky because if the highlight is one character off
 * it will fail, but the error messaged might still be correct. In this case it's likely the TextRange
 * is incorrect.
 */
public class CamelAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator/";
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
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidIntegerePropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorIntegerPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlInvalidIntegerePropertyTestData());
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

    private String getJavaInvalidBooleanPropertyTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaInvalidBooleanPropertyInProducerTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=false\")\n"
            + "                .to(\"file:test?allowNullBody=<error descr=\"Invalid boolean value: FISH\">FISH</error>\")\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaUnknownOptionsConsumerTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=false\")\n"
            + "                .to(\"file:test?allowNullBody=true&<error descr=\"Unknown option\">foo</error>=bar\")\n"
            + "        }\n"
            + "    }";
    }

    private String getJavaUnknownOptionsConsumerAnnotationTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "         @Consumer(file:test?allowNullBody=true&<error descr=\"Unknown option\">foo</error>=bar);"
            + "         public void onCheese(String name) {}"
            + "    }";
    }

    private String getJavaInvalidBooleanPropertyInProducerWithOpenUriTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
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

    private String getJavaInvalidIntegerePropertyTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?delay=<error descr=\"Invalid integer value: ImNotANumber\">ImNotANumber</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    private String getXmlInvalidIntegerePropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"timer:trigger?delay=<error descr=\"Invalid integer value: ImNotANumber\">ImNotANumber</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    private String getJavaInvalidReferencePropertyTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
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

}