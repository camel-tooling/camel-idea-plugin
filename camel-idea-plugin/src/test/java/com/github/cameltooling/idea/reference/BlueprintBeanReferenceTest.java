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
package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.reference.blueprint.BeanSelfReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.github.cameltooling.idea.reference.blueprint.ReferenceableIdPsiElement;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlueprintBeanReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String BLUEPRINT_NS = "http://www.osgi.org/xmlns/blueprint/v1.0.0";

    @Language("XML")
    private static final String PROPERTY_REFERENCE =
        "<blueprint xmlns='" + BLUEPRINT_NS + "'>" +
        "  <bean id='myBean' class='SomeClass'/>" +
        "  <bean id='otherBean' class='SomeClass'>" +
        "    <property name='prop' ref='<caret>myBean'/>" +
        "  </bean>" +
        "</blueprint>";

    @Language("XML")
    private static final String ROUTE_REFERENCE =
        "<blueprint xmlns='" + BLUEPRINT_NS + "'>" +
        "  <bean id='myBean' class='SomeClass'/>" +
        "  <camelContext xmlns='http://camel.apache.org/schema/blueprint'>" +
        "    <route>" +
        "      <from uri='direct:test'/>" +
        "      <bean ref='myB<caret>ean'/>" +
        "      <to uri='direct:xxx'/>" +
        "    </route>" +
        "  </camelContext>" +
        "</blueprint>";

    @Language("XML")
    private static final String MULTIPLE_REFERENCES =
        "<blueprint xmlns='" + BLUEPRINT_NS + "'>" +
            "  <bean id='myB<caret>ean' class='SomeClass'/>" +
            "  <bean id='otherBean' class='SomeClass'>" +
            "    <property name='prop' ref='myBean'/>" +
            "  </bean>" +
            "  <camelContext xmlns='http://camel.apache.org/schema/blueprint'>" +
            "    <route>" +
            "      <from uri='direct:test'/>" +
            "      <bean ref='myBean'/>" +
            "      <bean ref='myBean'/>" +
            "      <bean ref='myBean'/>" +
            "      <to uri='direct:xxx'/>" +
            "    </route>" +
            "  </camelContext>" +
            "</blueprint>";

    @Language("XML")
    private static final String ENDPOINT_REFERENCE =
        "<blueprint xmlns='" + BLUEPRINT_NS + "'>" +
        "  <bean id='otherBean' class='SomeClass'>" +
        "    <property name='prop' ref='my<caret>Endpoint'/>" +
        "  </bean>" +
        "  <camelContext xmlns='http://camel.apache.org/schema/blueprint'>" +
        "    <endpoint id='myEndpoint' uri='direct:test'/>" +
        "  </camelContext>" +
        "</blueprint>";

    @Language("XML")
    private static final String INVALID_REFERENCE =
        "<blueprint xmlns='" + BLUEPRINT_NS + "'>" +
        "  <bean id='myBean' class='SomeClass'/>" +
        "  <bean id='otherBean' class='SomeClass'>" +
        "    <property name='prop' ref='unknown<caret>Bean'/>" +
        "  </bean>" +
        "</blueprint>";

    public void testPropertyReference() {
        doTestReferenceAtCaret(PROPERTY_REFERENCE);
        expectResolvedBeanIdReferences(PROPERTY_REFERENCE, 1);
    }

    public void testRouteReference() {
        doTestReferenceAtCaret(ROUTE_REFERENCE);
    }

    public void testMultipleReferences() {
        configureFile(MULTIPLE_REFERENCES);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        Collection<UsageInfo> usages = findBeanUsages(element);
        assertEquals(5, usages.size());
        assertEquals(1, countUsages(usages, BeanSelfReference.class));
        assertEquals(4, countUsages(usages, BeanReference.class));
    }

    private long countUsages(Collection<UsageInfo> usages, Class referenceClass) {
        return usages.stream()
            .filter(u -> u.getReference() != null && u.getReference().getClass().equals(referenceClass))
            .count();
    }

    private Collection<UsageInfo> findBeanUsages(PsiElement element) {
        PsiReference reference = element.getReference();
        assertNotNull(reference);
        assertEquals(BeanSelfReference.class, reference.getClass());
        PsiElement target = reference.resolve();
        assertNotNull(target);
        assertEquals(ReferenceableIdPsiElement.class, target.getClass());
        return myFixture.findUsages(target);
    }

    public void testInvalidReference() {
        expectResolvedBeanIdReferences(INVALID_REFERENCE, 0);
    }

    public void testEndpointReference() {
        expectResolvedBeanIdReferences(ENDPOINT_REFERENCE, 1);
    }

    private void expectResolvedBeanIdReferences(String xml, int referenceCount) {
        configureFile(xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiElement> results = Arrays.stream(element.getReferences())
            .filter(r -> r instanceof BeanReference)
            .map(PsiReference::resolve)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        assertEquals(referenceCount, results.size());
    }

    private void doTestReferenceAtCaret(String xml) {
        configureFile(xml);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<XmlTag> results = TestReferenceUtil.resolveReference(element, XmlTag.class);
        assertEquals(1, results.size());
        XmlTag tag = results.get(0);
        assertEquals("bean", tag.getLocalName());
        assertEquals("myBean", tag.getAttributeValue("id", BLUEPRINT_NS));
    }

    private void configureFile(String multipleReferences) {
        myFixture.configureByText("context.xml", multipleReferences);
    }

}
