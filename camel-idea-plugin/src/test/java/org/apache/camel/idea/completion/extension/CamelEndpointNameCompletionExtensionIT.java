package org.apache.camel.idea.completion.extension;

import com.intellij.codeInsight.completion.CompletionType;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

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
        doTestDirectEndpointNameCompletion();
    }

    public void testDirectEndpointNameCompletionInXml() {
        myFixture.configureByFiles("CompleteDirectEndpointNameTestData.xml");
        myFixture.complete(CompletionType.BASIC);
    }

    private void doTestDirectEndpointNameCompletion() {
        myFixture.complete(CompletionType.BASIC);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(3, strings.size());
        assertContainsElements(strings, "direct:abc", "direct:def", "direct:test");
    }

}