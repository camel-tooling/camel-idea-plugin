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
package com.github.cameltooling.idea.completion.extension;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;
import org.intellij.lang.annotations.Language;

import java.util.List;

public class BlueprintFactoryMethodCompletionExtensionTest extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testBasicCompletionStaticFactoryMethods() {
        @Language("JAVA") String factory = """
            package test;
            public class FactoryA {
              public static String create() { return "x"; }
              public static String create(int x) { return "x"; }
              public String instanceOnly() { return "y"; }
              public static void ignoredVoid() {}
            }
            """;
        myFixture.addClass(factory);

        @Language("XML") String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='b' class='test.FactoryA' factory-method='cr<caret>eate'/>
            </blueprint>
            """;

        List<String> results = invokeCompletion(xml);
        assertNotNull(results);
        // overloaded "create" should appear only once due to de-duplication
        assertContainsElements(results, "create");
        assertEquals(1, results.stream().filter("create"::equals).count());
        // ensure instance-only method is not suggested for static factory context
        assertDoesntContain(results, "instanceOnly");
        // void-return method should be filtered out by BeanUtils
        assertDoesntContain(results, "ignoredVoid");
    }

    public void testBasicCompletionInstanceFactoryMethods() {
        @Language("JAVA") String factory = """
            package test;
            public class FactoryB {
              public String build() { return "x"; }
              public String build(int x) { return "x"; }
              public static String staticOnly() { return "s"; }
            }
            """;
        myFixture.addClass(factory);

        @Language("XML") String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='ref' class='test.FactoryB'/>
              <bean id='b' factory-ref='ref' factory-method='bu<caret>ild'/>
            </blueprint>
            """;

        List<String> results = invokeCompletion(xml);
        assertNotNull(results);
        assertContainsElements(results, "build");
        assertEquals(1, results.stream().filter("build"::equals).count());
        // static methods are not applicable for instance factory
        assertDoesntContain(results, "staticOnly");
    }

    public void testSmartCompletionFiltersByArguments() {
        @Language("JAVA") String factory = """
            package test;
            public class FactoryC {
              public static Object make(java.lang.String s) { return s; }
              public static Object make(Integer i) { return i; }
              public static Object make(String s, Integer i) { return s; }
            }
            """;
        myFixture.addClass(factory);

        @Language("XML") String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='b' class='test.FactoryC' factory-method='ma<caret>ke'>
                <argument type='java.lang.String'/>
              </bean>
            </blueprint>
            """;

        List<String> results = invokeSmartCompletion(xml);
        assertNotNull(results);
        // only the overload matching (String) should be suggested by SMART completion
        assertContainsElements(results, "make");
        assertEquals(1, results.size());
    }

    public void testNoCompletionOnNonFactoryAttribute() {
        @Language("XML") String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='my<caret>Bean' class='java.lang.String'/>
            </blueprint>
            """;
        List<String> results = invokeCompletion(xml);
        // In this location there should be no factory method proposals; be tolerant to null
        if (results != null) {
            assertEmpty(results);
        }
    }

    public void testCycleYieldsNoCompletions() {
        @Language("JAVA") String a = """
            package test;
            public class A { public B makeB() { return new B(); } }
            """;
        @Language("JAVA") String b = """
            package test;
            public class B { public A makeA() { return new A(); } }
            """;
        myFixture.addClass(a);
        myFixture.addClass(b);

        @Language("XML") String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='a' factory-ref='b' factory-method='ma<caret>keA'/>
              <bean id='b' factory-ref='a' factory-method='makeB'/>
            </blueprint>
            """;

        List<String> results = invokeCompletion(xml);
        // Cycle in factory beans should be handled and produce no suggestions
        if (results != null) {
            assertEmpty(results);
        }
    }

    private List<String> invokeCompletion(String xml) {
        myFixture.configureByText("context.xml", xml);
        myFixture.complete(CompletionType.BASIC);
        return myFixture.getLookupElementStrings();
    }

    private List<String> invokeSmartCompletion(String xml) {
        myFixture.configureByText("context.xml", xml);
        myFixture.complete(CompletionType.SMART);
        return myFixture.getLookupElementStrings();
    }

}
