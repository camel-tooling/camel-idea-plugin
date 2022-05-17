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
package com.github.cameltooling.idea.completion;

import java.util.Arrays;
import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.lookup.LookupElement;

/**
 * Testing the completion of the property values based on the options defined in the metadata of component, data format,
 * language and main.
 */
public class PropertyValueCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/property";
    }

    /**
     * Ensures that main option value suggestions can properly be proposed non filtered.
     */
    public void testMainOptionValueSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("main-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true", "false");
    }

    /**
     * Ensures that main option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testMainOptionValueSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("main-option-values"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that main option value suggestions can properly be proposed filtered.
     */
    public void testMainOptionValueSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("main-option-values"));
        myFixture.completeBasic();
        myFixture.type('t');
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true");
        assertDoesntContain(strings,  "false");
    }

    /**
     * Ensures that component option value suggestions can properly be proposed non filtered.
     */
    public void testComponentOptionValueSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("component-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true", "false");
    }

    /**
     * Ensures that component option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testComponentOptionValueSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("component-option-values"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that component option value suggestions can properly be proposed filtered.
     */
    public void testComponentOptionValueSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("component-option-values"));
        myFixture.completeBasic();
        myFixture.type('t');
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true");
        assertDoesntContain(strings,  "false");
    }

    /**
     * Ensures that data format option value suggestions can properly be proposed non filtered.
     */
    public void testDataFormatOptionValueSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("data-format-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true", "false");
    }

    /**
     * Ensures that data format option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testDataFormatOptionValueSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("data-format-option-values"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that data format option value suggestions can properly be proposed filtered.
     */
    public void testDataFormatOptionValueSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("data-format-option-values"));
        myFixture.completeBasic();
        myFixture.type('t');
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true");
        assertDoesntContain(strings,  "false");
    }

    /**
     * Ensures that language option value suggestions can properly be proposed non filtered.
     */
    public void testLanguageOptionValueSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("language-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "Singleton", "Request", "Prototype");
    }

    /**
     * Ensures that language option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testLanguageOptionValueSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("language-option-values"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that language option value suggestions can properly be proposed filtered.
     */
    public void testLanguageOptionValueSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("language-option-values"));
        myFixture.completeBasic();
        myFixture.type("si");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "Singleton");
        assertDoesntContain(strings,  "Request", "Prototype");
    }

    /**
     * Ensures that camel case is also supported.
     */
    public void testFilterInCamelCase() {
        myFixture.configureByFiles(getFileName("camel-case-value-filter"));
        myFixture.completeBasic();
        myFixture.type('t');
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true");
        assertDoesntContain(strings,  "false");
    }

    /**
     * Ensures that kebab case is also supported.
     */
    public void testFilterInKebabCase() {
        myFixture.configureByFiles(getFileName("kebab-case-value-filter"));
        myFixture.completeBasic();
        myFixture.type('f');
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "false");
        assertDoesntContain(strings,  "true");
    }

    /**
     * Ensures that no suggestions are provided when the first key is unknown.
     */
    public void testNoSuggestionOnUnknownFirstKey() {
        myFixture.configureByFiles(getFileName("value-unknown-first-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the second key is unknown.
     */
    public void testNoSuggestionOnUnknownSecondKey() {
        myFixture.configureByFiles(getFileName("value-unknown-second-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the component is unknown.
     */
    public void testNoSuggestionOnUnknownComponent() {
        myFixture.configureByFiles(getFileName("value-unknown-component"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }
    private String getFileName(String fileNamePrefix) {
        return String.format("%s.properties", fileNamePrefix);
    }
}
