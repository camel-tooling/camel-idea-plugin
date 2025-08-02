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
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import java.util.Arrays;
import java.util.List;

public class CamelPropertyPlaceholderReferenceContributorTest extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testNoReferencesForIncompletePlaceholders() {
        myFixture.configureByText("MyRoute.java", """
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("direct:abc?param1={{<caret>&param2=yyy")
                            .to("direct:test");
                    }
                }
                """);
        PsiElement textElement = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PropertyReference> refs = getPropertyReferences(textElement);
        assertEquals(1, refs.size()); // the whole string literal is one property reference, not contributed by us
        assertEquals("direct:abc?param1={{&param2=yyy", refs.getFirst().getCanonicalText());
    }

    public void testSingleReferenceInOtherStrings() {
        myFixture.configureByText("MyRoute.java", """
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("direct:abc")
                            .to("direct:abc")
                            .log("something {{pro<caret>p2}}");
                    }
                }
                """);
        PsiElement textElement = TestReferenceUtil.getParentElementAtCaret(myFixture);
        assertContainsPropertyReference(textElement, "prop2", 13);
    }

    public void testSingleReferenceInEndpointUri() {
        myFixture.configureByText("MyRoute.java", """
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("direct:a<caret>bc?p1={{myProp1}}&p2=xxx")
                            .to("direct:test");
                    }
                }
                """);
        PsiElement textElement = TestReferenceUtil.getParentElementAtCaret(myFixture);
        assertEquals(2, getPropertyReferences(textElement).size()); //the whole string literal is one property reference, +1 ours
        assertContainsPropertyReference(textElement, "myProp1", 17);
    }

    public void testMultipleReferences() {
        myFixture.configureByText("MyRoute.java", """
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("direct:a<caret>bc?p1={{myProp1}}&p2={{myProp2}}&p3={{myProp3}}")
                            .to("direct:test");
                    }
                }
                """);
        PsiElement textElement = TestReferenceUtil.getParentElementAtCaret(myFixture);
        assertEquals(4, getPropertyReferences(textElement).size()); //the whole string literal is one property reference, +3 ours
        assertContainsPropertyReference(textElement, "myProp1", 17);
        assertContainsPropertyReference(textElement, "myProp2", 32);
        assertContainsPropertyReference(textElement, "myProp3", 47);
    }

    public void testAttrInXml() {
        myFixture.configureByText("routes.xml", """
        <root>
            <route>
                <from uri="direct:abc?param1={{myPro<caret>p1}}&amp;param2=yyy"/>
                <to uri="direct:test"/>
            </route>
        </root>
        """);
        assertContainsPropertyReference(TestReferenceUtil.getParentElementAtCaret(myFixture), "myProp1", 21);
    }

    public void testTextInXml() {
        myFixture.configureByText("routes.xml", """
        <root>
            <route>
                <from uri="direct:abc?param1={{myProp1}}&amp;param2=yyy"/>
                <choice>
                    <when>
                        <simple>{{ft<caret>p.client}}</simple>
                        <to uri='direct:ftp'/>
                    </when>
                </choice>
            </route>
        </root>
        """);
        var xmlDataElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertContainsPropertyReference(xmlDataElement, "ftp.client", 2);
    }

    private void assertContainsPropertyReference(PsiElement caretElement, String propertyName, int refStartOffset) {
        List<PropertyReference> propertyRefs = getPropertyReferences(caretElement);
        PsiReference myPropRef = propertyRefs.stream().filter(r -> r.getCanonicalText().equals(propertyName))
                .findFirst()
                .orElse(null);
        assertNotNull("PropertyReference to " + propertyName + " not found in " + caretElement, myPropRef);
        assertEquals(new TextRange(refStartOffset, refStartOffset + propertyName.length()), myPropRef.getRangeInElement());
    }

    private static List<PropertyReference> getPropertyReferences(PsiElement caretElement) {
        PsiReference[] refs = caretElement.getReferences();
        return Arrays.stream(refs)
                .filter(ref -> ref instanceof PropertyReference)
                .map(ref -> (PropertyReference) ref)
                .toList();
    }


}