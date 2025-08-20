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
package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.reference.blueprint.model.FactoryBeanMethodReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlueprintFactoryMethodReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {


    public void testStaticFactoryMethodReference() {
        @Language("JAVA")
        String factoryClass = """
            package test;
            public class Factory {
              public static Foo create() { return new Foo(); }
            }
            """;
        @Language("JAVA")
        String productClass = """
            package test;
            public class Foo { }
            """;

        myFixture.addClass(factoryClass);
        myFixture.addClass(productClass);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='myBean' class='test.Factory' factory-method='cre<caret>ate'/>
            </blueprint>
            """;

        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);

        // Ensure our provider attached the proper reference type
        PsiReference[] refs = element.getReferences();
        assertEquals(1, refs.length);
        assertInstanceOf(refs[0], FactoryBeanMethodReference.class);

        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEquals(1, methods.size());
        assertEquals("create", methods.getFirst().getName());
        assertEquals("test.Factory", methods.getFirst().getContainingClass().getQualifiedName());
    }

    public void testInstanceFactoryMethodReference() {
        @Language("JAVA")
        String factoryClass = """
            package test;
            public class Factory {
              public Foo build() { return new Foo(); }
            }
            """;
        @Language("JAVA")
        String productClass = """
            package test;
            public class Foo { }
            """;

        myFixture.addClass(factoryClass);
        myFixture.addClass(productClass);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='factory' class='test.Factory'/>
              <bean id='product' factory-ref='factory' factory-method='buil<caret>d'/>
            </blueprint>
            """;

        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);

        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEquals(1, methods.size());
        PsiMethod method = methods.getFirst();
        assertEquals("build", method.getName());
        assertEquals("test.Factory", method.getContainingClass().getQualifiedName());
    }

    public void testNoReferenceOnNonFactoryMethodAttribute() {
        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='my<caret>Bean' class='java.lang.String'/>
            </blueprint>
            """;

        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        // other providers may add references (e.g., BeanSelfReference on id), ensure none are FactoryBeanMethodReference
        List<FactoryBeanMethodReference> refs = TestReferenceUtil.getReferencesOfType(element, FactoryBeanMethodReference.class);
        assertEmpty(refs);
    }

    public void testUnresolvedWhenMethodMissing() {
        @Language("JAVA")
        String clazz = """
            package test;
            public class Factory {
              public static String other() { return "x"; }
            }
            """;
        myFixture.addClass(clazz);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='myBean' class='test.Factory' factory-method='unkn<caret>own'/>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);

        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        // Provider still attaches reference, but it should not resolve to anything
        PsiReference[] refs = element.getReferences();
        assertEquals(1, refs.length);
        assertInstanceOf(refs[0], FactoryBeanMethodReference.class);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEmpty(methods);
    }

    public void testCycleInFactoryBeansLeadsToNoResolution() {
        // Two beans referring each other via factory-ref should produce a cycle and no valid results
        @Language("JAVA")
        String factoryA = """
            package test;
            public class A { public B makeB() { return new B(); } }
            """;
        @Language("JAVA")
        String factoryB = """
            package test;
            public class B { public A makeA() { return new A(); } }
            """;
        myFixture.addClass(factoryA);
        myFixture.addClass(factoryB);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='a' factory-ref='b' factory-method='make<caret>A'/>
              <bean id='b' factory-ref='a' factory-method='makeB'/>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);

        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        // Cycle should result in no valid resolve targets
        assertEmpty(methods);
    }

    public void testArgumentTypeAttributeOverloadFiltering() {
        @Language("JAVA")
        String factoryClass = """
            package test;
            public class Factory2 {
              public static Object make(java.lang.String s) { return s; }
              public static Object make(Integer i) { return i; }
              public static Object make(String s, Integer i) { return s; }
            }
            """;
        myFixture.addClass(factoryClass);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='b1' class='test.Factory2' factory-method='ma<caret>ke'>
                <argument type='java.lang.String'/>
              </bean>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEquals(1, methods.size());
        assertEquals("make", methods.getFirst().getName());
        assertEquals(1, methods.getFirst().getParameterList().getParametersCount());
        assertEquals("java.lang.String", getFirstParameterTypeName(methods));
    }

    private static @NotNull String getFirstParameterTypeName(List<PsiMethod> methods) {
        return methods.getFirst().getParameterList().getParameter(0).getType().getCanonicalText();
    }

    public void testArgumentValueUnknownTypeIsLenientByCount() {
        @Language("JAVA")
        String factoryClass = """
            package test;
            public class Factory3 {
              public static Object build(String s) { return s; }
              public static Object build(Integer i) { return i; }
              public static Object build(String s, Integer i) { return s; }
            }
            """;
        myFixture.addClass(factoryClass);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='b2' class='test.Factory3' factory-method='bui<caret>ld'>
                <argument value='42'/>
              </bean>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        // Unknown type but one argument -> both single-arg overloads should match
        assertEquals(2, methods.size());
        for (PsiMethod m : methods) {
            assertEquals("build", m.getName());
            assertEquals(1, m.getParameterList().getParametersCount());
        }
    }

    public void testArgumentRefResolvesBeanType() {
        @Language("JAVA")
        String classes = """
            package test;
            public class ArgA {}
            public class Factory4 {
              public static Object make(ArgA a) { return a; }
              public static Object make(String s) { return s; }
            }
            """;
        myFixture.addClass(classes);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='argA' class='test.ArgA'/>
              <bean id='b3' class='test.Factory4' factory-method='ma<caret>ke'>
                <argument ref='argA'/>
              </bean>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEquals(1, methods.size());
        assertEquals("test.ArgA", getFirstParameterTypeName(methods));
    }

    public void testNestedBeanArgumentType() {
        @Language("JAVA")
        String classes = """
            package test;
            public class ArgB {}
            public class Factory5 {
              public static Object create(ArgB b) { return b; }
              public static Object create(String s) { return s; }
            }
            """;
        myFixture.addClass(classes);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='b4' class='test.Factory5' factory-method='cre<caret>ate'>
                <argument>
                  <bean class='test.ArgB'/>
                </argument>
              </bean>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEquals(1, methods.size());
        assertEquals("test.ArgB", getFirstParameterTypeName(methods));
    }

    public void testMismatchedParameterCountNoResolution() {
        @Language("JAVA")
        String factoryClass = """
            package test;
            public class Factory6 {
              public static Object x(String s) { return s; }
            }
            """;
        myFixture.addClass(factoryClass);

        @Language("XML")
        String xml = """
            <blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>
              <bean id='b5' class='test.Factory6' factory-method='x<caret>'>
                <argument type='java.lang.String'/>
                <argument type='java.lang.Integer'/>
              </bean>
            </blueprint>
            """;
        myFixture.configureByText("context.xml", xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEmpty(methods);
    }
}
