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
import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
import com.github.cameltooling.idea.service.CamelPreferenceService;
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

    @Override
    protected void tearDown() throws Exception {
        try {
            CamelPreferenceService.getService().setCamelCatalogProvider(null);
        } finally {
            super.tearDown();
        }
    }

    /**
     * Ensures that main option value suggestions can properly be proposed non filtered.
     */
    public void testMainOptionValueSuggestionNonFiltered() {
        for (FileType type : FileType.values()) {
            testMainOptionValueSuggestionNonFiltered(type);
        }
    }

    private void testMainOptionValueSuggestionNonFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "main-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true", "false");
    }

    /**
     * Ensures that no suggestions are provided when the Camel Runtime is not compatible.
     */
    public void testNoOptionValueSuggestionWhenCamelRuntimeNotCompatible() {
        myFixture.configureByFiles(getFileName(FileType.YAML, "main-option-values", CamelCatalogProvider.DEFAULT));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that main option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testMainOptionValueSuggestionInstancesOfOptionSuggestion() {
        for (FileType type : FileType.values()) {
            testMainOptionValueSuggestionInstancesOfOptionSuggestion(type);
        }
    }

    private void testMainOptionValueSuggestionInstancesOfOptionSuggestion(FileType type) {
        myFixture.configureByFiles(getFileName(type, "main-option-values"));
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
        for (FileType type : FileType.values()) {
            testMainOptionValueSuggestionFiltered(type);
        }
    }

    private void testMainOptionValueSuggestionFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "main-option-values"));
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
        for (FileType type : FileType.values()) {
            testComponentOptionValueSuggestionNonFiltered(type);
        }
    }

    private void testComponentOptionValueSuggestionNonFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "component-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true", "false");
    }

    /**
     * Ensures that component option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testComponentOptionValueSuggestionInstancesOfOptionSuggestion() {
        for (FileType type : FileType.values()) {
            testComponentOptionValueSuggestionInstancesOfOptionSuggestion(type);
        }
    }

    private void testComponentOptionValueSuggestionInstancesOfOptionSuggestion(FileType type) {
        myFixture.configureByFiles(getFileName(type, "component-option-values"));
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
        for (FileType type : FileType.values()) {
            testComponentOptionValueSuggestionFiltered(type);
        }
    }

    private void testComponentOptionValueSuggestionFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "component-option-values"));
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
        for (FileType type : FileType.values()) {
            testDataFormatOptionValueSuggestionNonFiltered(type);
        }
    }

    private void testDataFormatOptionValueSuggestionNonFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "data-format-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "true", "false");
    }

    /**
     * Ensures that data format option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testDataFormatOptionValueSuggestionInstancesOfOptionSuggestion() {
        for (FileType type : FileType.values()) {
            testDataFormatOptionValueSuggestionInstancesOfOptionSuggestion(type);
        }
    }

    private void testDataFormatOptionValueSuggestionInstancesOfOptionSuggestion(FileType type) {
        myFixture.configureByFiles(getFileName(type, "data-format-option-values"));
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
        for (FileType type : FileType.values()) {
            testDataFormatOptionValueSuggestionFiltered(type);
        }
    }

    private void testDataFormatOptionValueSuggestionFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "data-format-option-values"));
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
        for (FileType type : FileType.values()) {
            testLanguageOptionValueSuggestionNonFiltered(type);
        }
    }

    private void testLanguageOptionValueSuggestionNonFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "language-option-values"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "Singleton", "Request", "Prototype");
    }

    /**
     * Ensures that language option value suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testLanguageOptionValueSuggestionInstancesOfOptionSuggestion() {
        for (FileType type : FileType.values()) {
            testLanguageOptionValueSuggestionInstancesOfOptionSuggestion(type);
        }
    }

    private void testLanguageOptionValueSuggestionInstancesOfOptionSuggestion(FileType type) {
        myFixture.configureByFiles(getFileName(type, "language-option-values"));
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
        for (FileType type : FileType.values()) {
            testLanguageOptionValueSuggestionFiltered(type);
        }
    }

    private void testLanguageOptionValueSuggestionFiltered(FileType type) {
        myFixture.configureByFiles(getFileName(type, "language-option-values"));
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
        for (FileType type : FileType.values()) {
            testFilterInCamelCase(type);
        }
    }

    private void testFilterInCamelCase(FileType type) {
        myFixture.configureByFiles(getFileName(type, "camel-case-value-filter"));
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
        for (FileType type : FileType.values()) {
            testFilterInKebabCase(type);
        }
    }

    private void testFilterInKebabCase(FileType type) {
        myFixture.configureByFiles(getFileName(type, "kebab-case-value-filter"));
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
        for (FileType type : FileType.values()) {
            testNoSuggestionOnUnknownFirstKey(type);
        }
    }

    private void testNoSuggestionOnUnknownFirstKey(FileType type) {
        myFixture.configureByFiles(getFileName(type, "value-unknown-first-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the second key is unknown.
     */
    public void testNoSuggestionOnUnknownSecondKey() {
        for (FileType type : FileType.values()) {
            testNoSuggestionOnUnknownSecondKey(type);
        }
    }

    private void testNoSuggestionOnUnknownSecondKey(FileType type) {
        myFixture.configureByFiles(getFileName(type, "value-unknown-second-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the component is unknown.
     */
    public void testNoSuggestionOnUnknownComponent() {
        for (FileType type : FileType.values()) {
            testNoSuggestionOnUnknownComponent(type);
        }
    }

    private void testNoSuggestionOnUnknownComponent(FileType type) {
        myFixture.configureByFiles(getFileName(type, "value-unknown-component"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    private static String getFileName(FileType type, String fileNamePrefix) {
        if (type == FileType.YAML) {
            // Switch to Quarkus mode
            return getFileName(type, fileNamePrefix, CamelCatalogProvider.QUARKUS);
        }
        return getFileName(type, fileNamePrefix, null);
    }
    private static String getFileName(FileType type, String fileNamePrefix, CamelCatalogProvider provider) {
        if (provider != null) {
            // Switch to Quarkus mode
            CamelPreferenceService.getService().setCamelCatalogProvider(provider);
        }
        return String.format("%s.%s", fileNamePrefix, type.name().toLowerCase());
    }

    enum FileType {
        PROPERTIES,
        YAML
    }
}
