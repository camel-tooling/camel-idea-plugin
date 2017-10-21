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
 * Test Camel simple validation and the expected value is highlighted
 * TIP : Writing highlighting test can be tricky because if the highlight is one character off
 * it will fail, but the error messaged might still be correct. In this case it's likely the TextRange
 * is incorrect.
 *
 * So far we can have been able to avoid pointing the -Didea.home.path=<location of Intellij CI source code>
 * because it's didn't really matter it could not resolve JDK classes when testing highlight. If you need
 * to resolve the JDK classes you will have to point the idea.home.path to the right location
 */
public class CamelSimpleAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator";
    }

    public void testAnnotatorSimpleValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithSimple());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorLogValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithLog());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorLogValidation2() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithFilterAndLog());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorOpenBracketSimpleValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaOpenBracketWithSimple());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorMultipleOpenBracketSimpleValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaMutlipleOpenBracketWithSimple());
        myFixture.checkHighlighting(false, false, true, true);
    }

    public void testAnnotatorCamelPredicateValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithCamelPredicate());
        myFixture.checkHighlighting(false, false, false, true);
    }

    public void testAnnotatorJavaMultilinePredicateValidation() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaMultilinePredicate());
        myFixture.checkHighlighting(false, false, false, true);
    }

    public void testAnnotatorCamelPredicateValidation2() {
        myFixture.configureByText("AnnotatorTestData.java", getJavaWithCamelPredicate2());
        myFixture.checkHighlighting(false, false, false, true);
    }

    public void testXmlAnnotatorSimpleValidation2() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlWithSimple());
        myFixture.checkHighlighting(false, false, false, true);
    }

    public void testXmlAnnotatorPredicateValidation2() {
        // TODO: A problem with IDEA not installed XSD schema for camel-spring.xsd which causes a highlight error
        // myFixture.configureByText("AnnotatorTestData.xml", getXmlWithPredicate());
        // myFixture.checkHighlighting(false, false, false, true);
    }

    public void testXmlAnnotatorWithLogValidation() {
        myFixture.configureByText("AnnotatorTestData.xml", getXmlWithLog());
        myFixture.checkHighlighting(false, false, false, true);
    }

    private String getJavaWithSimple() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"netty-http:http://localhost/cdi?matchOnUriPrefix=true&nettySharedHttpServer=#httpServer\")\n"
            + "            .id(\"http-route-cdi\")\n"
            + "            .transform()\n"
            + "            .simple(\"Response from Camel CDI on route<error descr=\"Unknown function: xrouteId\">${xrouteId}</error> using thread: ${threadName}\");"
            + "        }\n"
            + "    }";
    }

    private String getJavaWithLog() {
        return "import org.apache.camel.builder.RouteBuilder;"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "    public void configure() throws Exception {\n"
            + "      from(\"timer:stream?repeatCount=1\")\n"
            + "           .log(\"Result from query <error descr=\"Unknown function: xbody\">${xbody}</error>\")\n"
            + "           .process(exchange -> {\n"
            + "                exchange.getIn().setBody(Arrays.asList(\"fharms\"));\n"
            + "           .to(\"file:test.txt\");\n"
            + "    }"
            + " }";
    }

    private String getJavaWithFilterAndLog() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "    public void configure() throws Exception {\n"
            + "      from(\"direct:foo\")\n"
            + "         .filter(header(Exchange.REDELIVERED))\n"
            + "           .log(LoggingLevel.WARN, \"Processed ${body} after ${header.CamelRedeliveryCount} retries\")\n"
            + "           .to(\"mock:out\");\n"
            + "    }\n"
            + " }";
    }

    private String getJavaOpenBracketWithSimple() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"netty-http:http://localhost/cdi?matchOnUriPrefix=true&nettySharedHttpServer=#httpServer\")\n"
            + "            .id(\"http-route-cdi\")\n"
            + "            .transform()\n"
            + "            .simple(\"Response from Camel CDI on route${routeId} using thread: ${threadNam<error descr=\"expected symbol functionEnd but was eol\">e</error>\");"
            + "        }\n"
            + "    }";
    }

    private String getJavaMutlipleOpenBracketWithSimple() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"netty-http:http://localhost/cdi?matchOnUriPrefix=true&nettySharedHttpServer=#httpServer\")\n"
            + "            .id(\"http-route-cdi\")\n"
            + "            .transform()\n"
            + "            .simple(\"Response from Camel CDI on route${routeId} using thread: ${threadNam<error descr=\"expected symbol functionEnd but was eol\">e</error>\");"
            + "        }\n"
            + "    }";
    }

    private String getJavaWithCamelPredicate() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "              from(\"direct:start\")\n"
            + "                .loopDoWhile(simple(\"${body.length} <error descr=\"Unexpected token =\">=!=</error> 12\"))\n"
            + "                .to(\"mock:loop\")\n"
            + "                .end()\n"
            + "                .to(\"mock:result\");"
            + "        }\n"
            + "    }";
    }

    private String getJavaWithCamelPredicate2() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "              from(\"direct:start\")\n"
            + "                .loopDoWhile(simple(\"${body.length} != 12\"))\n"
            + "                .filter().simple(<error descr=\"Unknown function: xxxx\">\"${xxxx}\"</error>)\n"
            + "                .filter(simple(<error descr=\"Unknown function: yyyy\">\"${yyyy}\"</error>))\n"
            + "                .to(\"mock:loop\")\n"
            + "                .transform(body().append(\"A\"))\n"
            + "                .end()\n"
            + "                .to(\"mock:result\");"
            + "        }\n"
            + "    }";
    }

    private String getJavaMultilinePredicate() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + " from(\"timer:trigger\")\n"
            + "            .choice()\n"
            + "                .when(xpath(\"/person/city = 'London'\"))\n"
            + "                    .log(\"UK message\"+\n"
            + "                            \"with info <error descr=\"Unknown function: boxdy\">${boxdy}</error>\" +\n"
            + "                            \"file:${fxile:name}\")\n"
            + "                    .to(\"file:target/messages/uk\")\n"
            + "                .otherwise()\n"
            + "                    .log(\"Other message\")\n"
            + "                    .to(\"file:target/messages/others\");"
            + "        }\n"
            + "    }";
    }

    private String getXmlWithSimple() {
        return "<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n"
            + "  <route id=\"timerToInRoute\">\n"
            + "    <from uri=\"timer:foo?period=1s\"/>\n"
            + "    <transform>\n"
            + "      <simple>Message at <error descr=\"Unknown function: daxcdte:now:yyyy-MM-dd HH:mm:ss\">${daxcdte:now:yyyy-MM-dd HH:mm:ss}</error></simple>\n"
            + "    </transform>\n"
            + "    <to uri=\"activemq:queue:inbox\"/>\n"
            + "  </route>\n"
            + "</camelContext>";
    }

    private String getXmlWithLog() {
        return "<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n"
            + "  <route id=\"timerToInRoute\">\n"
            + "    <from uri=\"timer:foo?period=1s\"/>\n"
            + "         <log message=\"Hello <error descr=\"Unknown function: xbody\">${xbody}</error>\"/>\n"
            + "    <to uri=\"file:test.txt\"/>\n"
            + "  </route>\n"
            + "</camelContext>";
    }

    private String getXmlWithPredicate() {
        return "<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n"
            + "  <route id=\"foo\">\n"
            + "    <loop doWhile=\"true\">\n"
            + "      <simple>${body.length} &gt; 12</simple>\n"
            + "      <filter>\n"
            + "        <simple>${body.length} &g<error descr=\"Binary operator > does not support token t\">t; thousan</error>d</simple>\n"
            + "        <to uri=\"mock:high\"/>\n"
            + "      </filter>\n"
            + "      <filter>\n"
            + "        <simple>${body} contains <error descr=\"Unknown function: yyyy\">${yyyy}</error></simple>\n"
            + "        <to uri=\"mock:contain\"/>\n"
            + "      </filter>\n"
            + "    </loop>\n"
            + "    <to uri=\"mock:result\"/>"
            + "  </route>\n"
            + "</camelContext>";
    }
}