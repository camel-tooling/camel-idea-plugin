package com.github.cameltooling.idea.completion.extension;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;

import java.util.List;

/**
 * Testing camel endpoint name completion in Java and XML DSL
 */
public class CamelEndpointNameCompletionExtensionIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/endpointname";
    }

    public void testDirectEndpointNameCompletionInJava() {
        myFixture.configureByFiles("CompleteDirectEndpointNameTestData.java");
        doTestCompletion();
    }

    public void testDirectEndpointNameCompletionInXml() {
        myFixture.configureByFiles("CompleteDirectEndpointNameTestData.xml");
        myFixture.complete(CompletionType.BASIC);
        doTestCompletion();
    }

    public void testDirectEndpointNameCompletionInJavaAtInvalidPlace() {
        myFixture.configureByFiles("CompleteDirectEndpointNameAtInvalidPlace.java");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInXmlAtInvalidPlace() {
        myFixture.configureByFiles("CompleteDirectEndpointNameAtInvalidPlace.xml");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInNonUriAttribute() {
        myFixture.configureByFiles("CompleteDirectEndpointNameInNonUriAttribute.xml");
        doTestCompletionAtInvalidPlace();
    }

    public void testDirectEndpointNameCompletionInRouteStart() {
        myFixture.configureByFiles("CompleteDirectEndpointNameInRouteStart.xml");
        doTestCompletionAtInvalidPlace();
    }

    private void doTestCompletion() {
        myFixture.complete(CompletionType.BASIC);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(3, strings.size());
        assertContainsElements(strings, "direct:abc", "direct:def", "direct:test");
    }

    private void doTestCompletionAtInvalidPlace() {
        myFixture.complete(CompletionType.BASIC);
        List<String> strings = myFixture.getLookupElementStrings();
        if (strings != null) {
            assertFalse(strings.contains("direct:abc"));
            assertFalse(strings.contains("direct:def"));
            assertFalse(strings.contains("direct:test"));
            assertFalse(strings.contains("file:inbox"));
        }
    }

}