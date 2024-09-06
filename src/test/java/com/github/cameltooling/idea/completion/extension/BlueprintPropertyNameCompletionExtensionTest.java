package com.github.cameltooling.idea.completion.extension;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;

import java.util.List;

public class BlueprintPropertyNameCompletionExtensionTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/propertyname";
    }

    public void testPropertyCompletionAtCorrectLocation() {
        List<String> strings = invokeCompletionWithFile("PersonBean.xml", "Person.java");

        assertNotNull(strings);
        assertEquals(3, strings.size());
        assertContainsElements(strings, "name", "age", "propertyWithSetterOnly");
    }


    public void testPropertyCompletionAtIncorrectLocation() {
        List<String> strings = invokeCompletionWithFile("PersonBeanWithWrongLocation.xml", "Person.java");

        assertEmpty(strings);
    }

    private List<String> invokeCompletionWithFile(String ... files) {
        myFixture.configureByFiles(files);
        myFixture.complete(CompletionType.BASIC);
        return myFixture.getLookupElementStrings();
    }

}