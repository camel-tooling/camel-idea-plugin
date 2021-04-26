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
package com.github.cameltooling.idea.completion.extension;

import java.util.List;
import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;
import org.intellij.lang.annotations.Language;
import org.junit.Ignore;

/**
 * Testing bean reference completion in blueprint files
 */
public class BeanReferenceCompletionExtensionIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/gutter/beaninject";
    }

    @Language("XML")
    private static final String PROPERTY_REFERENCE =
        "<blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>" +
        "  <bean id='bean1' class='SomeClass1'/>" +
        "  <bean id='bean2' class='SomeClass2'/>" +
        "  <bean id='xxx' class='SomeClass2'/>" +
        "  <bean id='testboo' class='SomeClass3'/>" +
        "  <bean id='boo' class='SomeClass4'>" +
        "    <property name='prop' ref='b<caret>'/>" +
        "  </bean>" +
        "</blueprint>";

    @Language("XML")
    private static final String ROUTE_REFERENCE =
        "<blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>" +
        "  <bean id='myBean' class='SomeClass'/>" +
        "  <bean id='myBean2' class='SomeClass'/>" +
        "  <bean id='myFoo' class='SomeClass'/>" +
        "  <camelContext xmlns='http://camel.apache.org/schema/blueprint'>" +
        "    <route>" +
        "      <from uri='direct:test'/>" +
        "      <bean ref='myB<caret>'/>" +
        "      <to uri='direct:xxx'/>" +
        "    </route>" +
        "  </camelContext>" +
        "</blueprint>";

    @Language("XML")
    private static final String ENDPOINT_REFERENCE =
        "<blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>" +
        "  <bean id='myBean' class='SomeClass'/>" +
        "  <camelContext xmlns='http://camel.apache.org/schema/blueprint'>" +
        "    <endpoint id='myEndpoint' uri='direct:test'/>" +
        "    <route>" +
        "      <from uri='direct:test'/>" +
        "      <to ref='my<caret>'/>" +
        "    </route>" +
        "  </camelContext>" +
        "</blueprint>";

    @Language("XML")
    private static final String NO_REFERENCE =
        "<blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>" +
        "  <bean id='myBean' class='SomeClass'/>" +
        "  <bean id='myBean2' class='SomeClass'/>" +
        "  <bean id='myFoo' class='SomeClass'/>" +
        "  <camelContext xmlns='http://camel.apache.org/schema/blueprint'>" +
        "    <route>" +
        "      <from uri='direct:test'/>" +
        "      <to uri='myF<caret>'/>" +
        "    </route>" +
        "  </camelContext>" +
        "</blueprint>";

    @Ignore
    public void testBeanInjectValue() {
        myFixture.configureByFiles("TestClass1.java", "TestClass2.java", "TestClass3.java", "beans.xml");
        myFixture.complete(CompletionType.BASIC);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertContainsElements(strings, "testClass2Bean", "testClass2Bean2");
    }

    public void testPropertyReference() {
        List<String> strings = doTestCompletionAtCaret(PROPERTY_REFERENCE);
        assertNotNull(strings);
        assertEquals(3, strings.size());
        assertContainsElements(strings, "bean1", "bean2", "boo");
    }

    public void testRouteReference() {
        List<String> strings = doTestCompletionAtCaret(ROUTE_REFERENCE);
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertContainsElements(strings, "myBean", "myBean2");
    }

    public void testEndpointReference() {
        List<String> strings = doTestCompletionAtCaret(ENDPOINT_REFERENCE);
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertContainsElements(strings, "myBean", "myEndpoint");
    }

    public void testNoResultsWhenUsedAsEndpoint() {
        List<String> strings = doTestCompletionAtCaret(NO_REFERENCE);
        assertNull(strings);
    }

    private List<String> doTestCompletionAtCaret(String routeReference) {
        myFixture.configureByText("test.xml", routeReference);
        myFixture.complete(CompletionType.BASIC);
        return myFixture.getLookupElementStrings();
    }

}
