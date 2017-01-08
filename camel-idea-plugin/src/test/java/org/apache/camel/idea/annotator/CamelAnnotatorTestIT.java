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

import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

/**
 * Test Camel URI validation and the expected value is highlighted
 * TODO : Still need to find out how we can make a positive test without it complaining about it can't find SDK classes
 */
public class CamelAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator/";
    }

    public String getJavaInvalidBooleanPropertyTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=DDDD\")\n"
            + "            from(\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    public String getXmlInvalidBooleanPropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"timer:trigger?bridgeErrorHandler=DDDD\"/>\n"
            + "      <from uri=\"timer:trigger?bridgeErrorHandler=<error descr=\"Invalid boolean value: DDDD\">DDDD</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    public String getJavaInvalidIntegerePropertyTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?delay=ImNotANumber\")\n"
            + "            from(\"timer:trigger?delay=<error descr=\"Invalid integer value: ImNotANumber\">ImNotANumber</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    public String getXmlInvalidIntegerePropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"timer:trigger?delay=ImNotANumber\"/>\n"
            + "      <from uri=\"timer:trigger?delay=<error descr=\"Invalid integer value: ImNotANumber\">ImNotANumber</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }

    public String getJavaInvalidReferencePropertyTestData() {
        return "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"jms:queue:myqueue?jmsKeyFormatStrategy=foo\")\n"
            + "            from(\"jms:queue:myqueue?jmsKeyFormatStrategy=<error descr=\"Invalid enum value: foo. Possible values: "
            + "[default, passthrough]. Did you mean: [default, passthrough]\">foo</error>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";
    }

    public String getXmlInvalidReferencePropertyTestData() {
        return "<route id=\"generateOrder-route\">\n"
            + "      <from uri=\"jms:queue:myqueue?jmsKeyFormatStrategy=foo\"/>\n"
            + "      <from uri=\"jms:queue:myqueue?jmsKeyFormatStrategy=<error descr=\"Invalid enum value: foo. Possible values: "
            + "[default, passthrough]. Did you mean: [default, passthrough]\">foo</error>\"/>\n"
            + "      <to uri=\"file:outbox\"/>\n"
            + "    </route>";
    }


    public void testAnnotatorInvalidBooleanPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidBooleanPropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorInvalidBooleanPropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlInvalidBooleanPropertyTestData());
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

    public void testAnnotatorReferencePropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaInvalidReferencePropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testXmlAnnotatorReferencePropertyValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlInvalidReferencePropertyTestData());
        myFixture.checkHighlighting(false, false, true, true);
    }

}