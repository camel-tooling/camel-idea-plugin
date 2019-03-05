package org.apache.camel.idea.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.reference.endpoint.direct.DirectEndpointPsiElement;
import org.apache.camel.idea.reference.endpoint.direct.DirectEndpointReference;
import org.intellij.lang.annotations.Language;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
public class CamelDirectEndpointReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Language("Java")
    private static final String JAVA_ROUTE_WITH_REFERENCE =
        "import org.apache.camel.builder.RouteBuilder;" +
        "public final class CompleteDirectEndpointName1TestData extends RouteBuilder {" +
        "    @Override" +
        "    public void configure() {" +
        "        from(\"direct:abc?param1=xxx\")" +
        "            .to(\"direct:<caret>test\");" +
        "        from(\"direct:def\")" +
        "            .to(\"direct:test\");" +
        "        from(\"direct:test\")" +
        "            .to(\"direct:def\");" +
        "    }" +
        "}";

    @Language("Java")
    private static final String JAVA_NO_EXTRA_REFERENCE =
        "import org.apache.camel.builder.RouteBuilder;" +
        "public final class CompleteDirectEndpointName1TestData extends RouteBuilder {" +
        "    @Override" +
        "    public void configure() {" +
        "        from(\"direct:abc?param1=xxx\")" +
        "            .to(\"direct:test\");" +
        "        from(\"direct:def\")" +
        "            .to(\"direct:<caret>xxx\");" +
        "        from(\"direct:test\")" +
        "            .to(\"direct:def\");" +
        "    }" +
        "}";

    @Language("XML")
    private static final String XML_ROUTE_WITH_REFERENCE =
        "<camelContext xmlns=\"http://camel.apache.org/schema/blueprint\">" +
        "  <route>" +
        "    <from uri=\"direct:abc?param1=xxx\"/>" +
        "    <to uri=\"direct:xxx\"/>" +
        "  </route>" +
        "  <route>" +
        "    <from uri=\"direct:def\"/>" +
        "    <to uri=\"direct:test\"/>" +
        "  </route>" +
        "  <route>" +
        "    <from uri=\"direct:test\"/>" +
        "    <to uri=\"direct:<caret>def\"/>" +
        "  </route>" +
        "</camelContext>";

    @Language("XML")
    private static final String XML_ROUTE_WITH_MULTIPLE_REFERENCES =
        "<camelContext xmlns=\"http://camel.apache.org/schema/blueprint\">" +
        "  <route>" +
        "    <from uri=\"direct:abc\"/>" +
        "    <to uri=\"direct:test\"/>" +
        "  </route>" +
        "  <route>" +
        "    <from uri=\"direct:abc\"/>" +
        "    <to uri=\"direct:test\"/>" +
        "  </route>" +
        "  <route>" +
        "    <from uri=\"direct:test\"/>" +
        "    <to uri=\"direct:<caret>abc\"/>" +
        "  </route>" +
        "</camelContext>";

    public void testJavaDirectEndpointReference() {
        myFixture.configureByText("RouteWithReferences.java", JAVA_ROUTE_WITH_REFERENCE);
        PsiElement element = getParentElementAtCaret();
        ResolveResult[] results = resolveDirectReference(element);
        assertEquals(1, results.length);

        PsiMethodCallExpression methodCall = resolveTarget(results[0], PsiMethodCallExpression.class);
        assertNotNull(methodCall);
        assertEquals("from(\"direct:test\")", methodCall.getText());
    }

    private <T extends PsiElement> T resolveTarget(ResolveResult result, Class<T> targetClass) {
        PsiElement resolvedElement = result.getElement();
        assertTrue(resolvedElement instanceof DirectEndpointPsiElement);
        PsiElement target = ((DirectEndpointPsiElement) resolvedElement).getNavigationElement();
        return PsiTreeUtil.getParentOfType(target, targetClass);
    }

    public void testJavaNoExtraReferences() {
        myFixture.configureByText("RouteWithReferences.java", JAVA_NO_EXTRA_REFERENCE);
        PsiElement element = getParentElementAtCaret();
        ResolveResult[] results = resolveDirectReference(element);
        assertEquals(0, results.length);
    }

    public void testXmlDirectEndpointReference() {
        myFixture.configureByText("route-with-references.xml", XML_ROUTE_WITH_REFERENCE);
        PsiElement element = getParentElementAtCaret();
        ResolveResult[] results = resolveDirectReference(element);
        assertEquals(1, results.length);

        XmlTag tag = resolveTarget(results[0], XmlTag.class);
        assertEquals("<from uri=\"direct:def\"/>", tag.getText());
    }

    public void testXmlMultipleReferences() {
        myFixture.configureByText("route-with-references.xml", XML_ROUTE_WITH_MULTIPLE_REFERENCES);
        PsiElement element = getParentElementAtCaret();
        ResolveResult[] results = resolveDirectReference(element);
        assertEquals(2, results.length);
    }

    private ResolveResult[] resolveDirectReference(PsiElement element) {
        PsiReference[] references = element.getReferences();
        assertEquals(1, references.length);
        PsiReference reference = references[0];
        assertTrue(reference instanceof DirectEndpointReference);

        return ((DirectEndpointReference) reference).multiResolve(false);
    }

    private PsiElement getParentElementAtCaret() {
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(element);
        return element.getParent();
    }

}
