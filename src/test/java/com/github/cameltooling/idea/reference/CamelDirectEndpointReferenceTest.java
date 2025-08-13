package com.github.cameltooling.idea.reference;

import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
public class CamelDirectEndpointReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MODEL_MAVEN_ARTIFACT};
    }

    @Language("Java")
    private static final String JAVA_ROUTE_WITH_REFERENCE =
            """
            import org.apache.camel.builder.RouteBuilder;
            public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
                @Override
                public void configure() {
                    from("direct:abc?param1=xxx")
                        .to("direct:<caret>test");
                    from("direct:def")
                        .to("direct:test");
                    from("direct:test")
                        .to("direct:def");
                }
            }
            """;

    @Language("Java")
    private static final String JAVA_NO_EXTRA_REFERENCE =
            """
            import org.apache.camel.builder.RouteBuilder;
            public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
                @Override
                public void configure() {
                    from("direct:abc?param1=xxx")
                        .to("direct:test");
                    from("direct:def")
                        .to("direct:<caret>xxx");
                    from("direct:test")
                        .to("direct:def");
                }
            }""";

    @Language("XML")
    private static final String XML_ROUTE_WITH_REFERENCE =
            """
            <camelContext xmlns="http://camel.apache.org/schema/blueprint">
              <route>
                <from uri="direct:abc?param1=xxx"/>
                <to uri="direct:xxx"/>
              </route>
              <route>
                <from uri="direct:def"/>
                <to uri="direct:test"/>
              </route>
              <route>
                <from uri="direct:test"/>
                <to uri="direct:<caret>def"/>
              </route>
            </camelContext>""";

    @Language("XML")
    private static final String XML_ROUTE_WITH_MULTIPLE_REFERENCES =
            """
            <camelContext xmlns="http://camel.apache.org/schema/blueprint">
              <route>
                <from uri="direct:abc"/>
                <to uri="direct:test"/>
              </route>
              <route>
                <from uri="direct:abc"/>
                <to uri="direct:test"/>
              </route>
              <route>
                <from uri="direct:test"/>
                <to uri="direct:<caret>abc"/>
              </route>
            </camelContext>""";

    public void testJavaDirectEndpointReference() {
        myFixture.configureByText("RouteWithReferences.java", JAVA_ROUTE_WITH_REFERENCE);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethodCallExpression> results = TestReferenceUtil.resolveReference(element, PsiMethodCallExpression.class);
        assertEquals(1, results.size());
        assertEquals("from(\"direct:test\")", results.get(0).getText());
    }

    public void testJavaNoExtraReferences() {
        myFixture.configureByText("RouteWithReferences.java", JAVA_NO_EXTRA_REFERENCE);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiElement> results = TestReferenceUtil.resolveReference(element);
        assertTrue(results.isEmpty());
    }

    public void testXmlDirectEndpointReference() {
        myFixture.configureByText("route-with-references.xml", XML_ROUTE_WITH_REFERENCE);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<XmlTag> results = TestReferenceUtil.resolveReference(element, XmlTag.class);
        assertEquals(1, results.size());
        assertEquals("<from uri=\"direct:def\"/>", results.get(0).getText());
    }

    public void testXmlMultipleReferences() {
        myFixture.configureByText("route-with-references.xml", XML_ROUTE_WITH_MULTIPLE_REFERENCES);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<XmlAttributeValue> values = TestReferenceUtil.resolveReference(element, XmlAttributeValue.class);
        assertEquals(2, values.size());
        for (XmlAttributeValue value : values) {
            assertEquals("direct:abc", value.getValue());
        }
    }

    public void testRouteInRestEndpoint() {
        myFixture.configureByText("route-with-rest-endpoint.xml", """
        <camelContext xmlns="http://camel.apache.org/schema/spring">
            <rest path="/say">
                <get path="/hello">
                    <to uri="direct:<caret>hello"/>
                </get>
                <get path="/bye" consumes="application/json">
                    <to uri="direct:bye"/>
                </get>
                <post path="/bye">
                    <to uri="mock:update"/>
                </post>
            </rest>
            <route>
                <from uri="direct:hello"/>
                <to uri="file:outbox"/>
            </route>
        </camelContext>
        """);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<XmlAttributeValue> values = TestReferenceUtil.resolveReference(element, XmlAttributeValue.class);
        assertEquals(1, values.size());
        assertEquals("direct:hello", values.getFirst().getValue());
    }

    public void testOnExceptionClause() {
        myFixture.configureByText("RouteWithOnException.java",  """
            import org.apache.camel.builder.RouteBuilder;
            public final class RouteWithOnException extends RouteBuilder {
                @Override
                public void configure() {
                    onException(Exception.class)
                        .to("direct:d<caret>ef");
                    from("direct:abc?param1=xxx")
                        .to("direct:test");
                    from("direct:def")
                        .to("direct:xxx");
                    from("direct:test")
                        .to("direct:def");
                }
            }""");

        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethodCallExpression> results = TestReferenceUtil.resolveReference(element, PsiMethodCallExpression.class);
        assertEquals(1, results.size());
        assertEquals("from(\"direct:def\")", results.getFirst().getText());
    }

    public void testOnCompletionClause() {
        myFixture.configureByText("RouteWithOnException.java",  """
            import org.apache.camel.builder.RouteBuilder;
            public final class RouteWithOnException extends RouteBuilder {
                @Override
                public void configure() {
                    onCompletion(Exception.class)
                        .to("direct:d<caret>ef");
                    from("direct:abc?param1=xxx")
                        .to("direct:test");
                    from("direct:def")
                        .to("direct:xxx");
                    from("direct:test")
                        .to("direct:def");
                }
            }""");

        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiMethodCallExpression> results = TestReferenceUtil.resolveReference(element, PsiMethodCallExpression.class);
        assertEquals(1, results.size());
        assertEquals("from(\"direct:def\")", results.getFirst().getText());
    }


}
