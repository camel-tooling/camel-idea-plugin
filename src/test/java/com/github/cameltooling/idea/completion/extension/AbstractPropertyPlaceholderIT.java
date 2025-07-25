package com.github.cameltooling.idea.completion.extension;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupEx;
import org.intellij.lang.annotations.MagicConstant;

import java.util.List;

public abstract class AbstractPropertyPlaceholderIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        super.initCamelPreferencesService();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        super.initCamelPreferencesService();
    }

    @Override
    protected String getTestDataPath() {
        return super.getTestDataPath() + "completion/propertyplaceholder/";
    }

    void runCompletionTest(String expectedFile, List<String> expectedLookupElements) {
        runCompletionTest(expectedFile, expectedLookupElements, Lookup.REPLACE_SELECT_CHAR);
    }

    void runCompletionTest(String expectedFile, List<String> expectedLookupElements, String selectedLookupElement) {
        runCompletionTest(expectedFile, expectedLookupElements, Lookup.REPLACE_SELECT_CHAR, selectedLookupElement);
    }

    void runCompletionTest(String expectedFile, List<String> expectedLookupElements,
                           @MagicConstant(valuesFromClass = Lookup.class) char lookupChar) {
        runCompletionTest(expectedFile, expectedLookupElements, lookupChar, null);
    }

    void runCompletionTest(String expectedFile, List<String> expectedLookupElements,
                           @MagicConstant(valuesFromClass = Lookup.class) char lookupChar,
                           String selectedLookupElement) {
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertOrderedEquals(strings, expectedLookupElements);

        if (selectedLookupElement != null) {
            LookupEx lookup = myFixture.getLookup();
            lookup.setCurrentItem(lookup.getItems().stream()
                    .filter(item -> item.getLookupString().equals(selectedLookupElement))
                    .findFirst().orElseThrow());
        }

        myFixture.finishLookup(lookupChar);
        myFixture.checkResultByFile(expectedFile);
    }

}
