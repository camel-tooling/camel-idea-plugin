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
 * Testing the completion of the property keys based on the options defined in the metadata of component, data format,
 * language and main in properties files.
 */
public class YamlPropertyKeyCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String SUFFIX = String.format("%n");
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
     * Ensures that no suggestions are provided when the partial first key is unknown.
     */
    public void testNoSuggestionOnUnknownFirstPartialKey() {
        myFixture.configureByFiles(getFileName("unknown-partial-first-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the first key is unknown.
     */
    public void testNoSuggestionOnUnknownFirstKey() {
        myFixture.configureByFiles(getFileName("unknown-first-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the second key is unknown.
     */
    public void testNoSuggestionOnUnknownSecondKey() {
        myFixture.configureByFiles(getFileName("unknown-second-key"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that no suggestions are provided when the component is unknown.
     */
    public void testNoSuggestionOnUnknownComponent() {
        myFixture.configureByFiles(getFileName("unknown-component"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that suggestions of groups are possible in an empty file.
     */
    public void testGroupSuggestionInEmptyFile() {
        testSuggestionWhenEmptyKey("empty");
    }

    /**
     * Ensures that suggestions of groups are possible in a non-empty file.
     */
    public void testGroupSuggestionInNonEmptyFile() {
        testSuggestionWhenEmptyKey("non-empty");
    }

    /**
     * Ensures that group suggestions are properly filtered when part of the first key section is provided.
     */
    public void testGroupSuggestionWithPartialFirstKey() {
        myFixture.configureByFiles(getFileName("empty"));
        myFixture.completeBasic();
        myFixture.type("cam");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel:" + SUFFIX);
    }

    /**
     * Ensures that group suggestions are properly filtered when the first key section is fully provided but without
     * the separator.
     */
    public void testGroupSuggestionWithFullFirstKeyWithoutSeparator() {
        myFixture.configureByFiles(getFileName("empty"));
        myFixture.completeBasic();
        myFixture.type("camel");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel:" + SUFFIX);
    }

    /**
     * Ensures that group suggestions are properly filtered when the first key section is fully provided but with
     * the separator.
     */
    public void testGroupSuggestionWithFullFirstKeyWithSeparator() {
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "main:" + SUFFIX, "component:" + SUFFIX, "language:" + SUFFIX, "dataformat:" + SUFFIX);
    }

    /**
     * Ensures that group suggestions for default Camel Runtime matches with the expectations.
     */
    public void testGroupSuggestionForDefaultCamelRuntime() {
        CamelPreferenceService.getService().setCamelCatalogProvider(CamelCatalogProvider.DEFAULT);
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that group suggestions for Quarkus Camel Runtime matches with the expectations.
     */
    public void testGroupSuggestionForQuarkusCamelRuntime() {
        CamelPreferenceService.getService().setCamelCatalogProvider(CamelCatalogProvider.QUARKUS);
        testGroupSuggestionWithFullFirstKeyWithSeparator();
    }

    /**
     * Ensures that group suggestions for Karaf Camel Runtime matches with the expectations.
     */
    public void testGroupSuggestionForKarafCamelRuntime() {
        CamelPreferenceService.getService().setCamelCatalogProvider(CamelCatalogProvider.KARAF);
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNullOrEmpty(strings);
    }

    /**
     * Ensures that group suggestions are properly filtered when part of the second key section is provided.
     */
    public void testGroupSuggestionWithPartialSecondKey() {
        testSuggestionWithSecondKey("mai");
    }

    /**
     * Ensures that group suggestions are properly filtered when the second key section is provided.
     */
    public void testGroupSuggestionWithSecondKey() {
        testSuggestionWithSecondKey("main");
    }

    private void testSuggestionWithSecondKey(String type) {
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        myFixture.type(type);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "main:" + SUFFIX);
        assertDoesntContain(strings, "component:" + SUFFIX, "language:" + SUFFIX, "dataformat:" + SUFFIX);
    }

    /**
     * Ensures that group name suggestions are only instances of {@link SimpleSuggestion}.
     */
    public void testGroupNameSuggestionInstancesOfSimpleSuggestion() {
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        myFixture.type("main");
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of SimpleSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof SimpleSuggestion)
        );
    }

    /**
     * Ensures that main option suggestions can properly be proposed non filtered.
     */
    public void testMainOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("main-options-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "debugging: ", "configurations: ", "auto-startup: ");
    }

    /**
     * Ensures that main option suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testMainOptionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("main-options-non-filtered"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that main option suggestions can properly be proposed filtered.
     */
    public void testMainOptionSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("main-options-non-filtered"));
        myFixture.completeBasic();
        myFixture.type("deb");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "debugging: ");
        assertDoesntContain(strings, "configurations: ", "auto-startup: ");
    }

    /**
     * Ensures that component name suggestions can properly be proposed non filtered.
     */
    public void testComponentNameSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("component-names-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "ftp:" + SUFFIX, "bean:" + SUFFIX, "cql:" + SUFFIX);
    }

    /**
     * Ensures that component name suggestions are only instances of {@link SimpleSuggestion}.
     */
    public void testComponentNameSuggestionInstancesOfSimpleSuggestion() {
        myFixture.configureByFiles(getFileName("component-names-non-filtered"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of SimpleSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof SimpleSuggestion)
        );
    }

    /**
     * Ensures that component name suggestions can properly be proposed filtered.
     */
    public void testComponentNameSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("component-names-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "ftp:" +  SUFFIX);
        assertDoesntContain(strings, "bean:" +  SUFFIX, "cql:" +  SUFFIX);
    }

    /**
     * Ensures that component option suggestions can properly be proposed non filtered.
     */
    public void testComponentOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("component-options-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "scheduler: ", "enable-jmx: ", "properties: ");
    }

    /**
     * Ensures that component option suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testComponentOptionSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("component-options-non-filtered"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that component options suggestions can properly be proposed filtered.
     */
    public void testComponentOptionSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("component-options-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "scheduler: ");
        assertDoesntContain(strings, "enable-jmx: ", "properties: ");
    }

    /**
     * Ensures that language name suggestions can properly be proposed non filtered.
     */
    public void testLanguageNameSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("language-names"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "jsonpath:" + SUFFIX, "bean:" + SUFFIX, "xpath:" + SUFFIX);
    }

    /**
     * Ensures that language name suggestions are only instances of {@link SimpleSuggestion}.
     */
    public void testLanguageNameSuggestionInstancesOfSimpleSuggestion() {
        myFixture.configureByFiles(getFileName("language-names"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of SimpleSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof SimpleSuggestion)
        );
    }

    /**
     * Ensures that language name suggestions can properly be proposed filtered.
     */
    public void testLanguageNameSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("language-names"));
        myFixture.completeBasic();
        myFixture.type("be");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "bean:" + SUFFIX);
        assertDoesntContain(strings, "jsonpath:" + SUFFIX, "xpath:" + SUFFIX);
    }

    /**
     * Ensures that language option suggestions can properly be proposed non filtered.
     */
    public void testLanguageOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("language-options"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "method: ", "scope: ", "bean-type: ");
        assertDoesntContain(strings, "id: ");
    }

    /**
     * Ensures that language option suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testLanguageOptionSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("language-options"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that language option suggestions can properly be proposed filtered.
     */
    public void testLanguageOptionSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("language-options"));
        myFixture.completeBasic();
        myFixture.type("sc");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "scope: ");
        assertDoesntContain(strings, "method: ", "bean-type: ");
    }

    /**
     * Ensures that data format name suggestions can properly be proposed non filtered.
     */
    public void testDataFormatNameSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("data-format-names-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "jackson:" + SUFFIX, "csv:" + SUFFIX, "bindyCsv:" + SUFFIX);
    }

    /**
     * Ensures that data format name suggestions are only instances of {@link SimpleSuggestion}.
     */
    public void testDataFormatNameSuggestionInstancesOfSimpleSuggestion() {
        myFixture.configureByFiles(getFileName("data-format-names-non-filtered"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of SimpleSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof SimpleSuggestion)
        );
    }

    /**
     * Ensures that data format name suggestions can properly be proposed filtered.
     */
    public void testDataFormatNameSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("data-format-names-non-filtered"));
        myFixture.completeBasic();
        myFixture.type("cs");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "csv:" + SUFFIX);
        assertDoesntContain(strings, "jackson:" + SUFFIX, "xpath:" + SUFFIX);
    }

    /**
     * Ensures that data format option suggestions can properly be proposed non filtered.
     */
    public void testDataFormatOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("data-format-options"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "include: ", "pretty-print: ", "json-view: ");
        assertDoesntContain(strings, "id: ");
    }

    /**
     * Ensures that data format option suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testDataFormatOptionSuggestionInstancesOfOptionSuggestion() {
        myFixture.configureByFiles(getFileName("data-format-options"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions).map(LookupElement::getObject).anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that data format option suggestions can properly be proposed filtered.
     */
    public void testDataFormatOptionSuggestionFiltered() {
        myFixture.configureByFiles(getFileName("data-format-options"));
        myFixture.completeBasic();
        myFixture.type("inc");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "include: ");
        assertDoesntContain(strings, "pretty-print: ", "json-view: ");
    }

    /**
     * Ensures that camel case is also supported.
     */
    public void testFilterInCamelCase() {
        myFixture.configureByFiles(getFileName("camel-case-key-filter"));
        myFixture.completeBasic();
        myFixture.type("allowe");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "allow-empty-stream: ");
        assertDoesntContain(strings, "class-type: ", "local: ");
    }

    /**
     * Ensures that kebab case is also supported.
     */
    public void testFilterInKebabCase() {
        myFixture.configureByFiles(getFileName("kebab-case-key-filter"));
        myFixture.completeBasic();
        myFixture.type("access-K");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "access-key: ");
        assertDoesntContain(strings, "query-string: ", "output-location: ");
    }

    private void testSuggestionWhenEmptyKey(String fileNamePrefix) {
        myFixture.configureByFiles(getFileName(fileNamePrefix));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel:" + SUFFIX);
    }

    /**
     * Ensure that the property completion has expected indent in case of intermediate key.
     */
    public void testPropertyCompletionIntermediateKey() {
        myFixture.configureByFiles(getFileName("component-names-filtered"));
        myFixture.completeBasic();
        myFixture.type('\n');
        myFixture.type('x');
        myFixture.checkResultByFile(getFileName("component-names-filtered-result"));
    }

    /**
     * Ensure that the property completion has expected indent in case of end of key.
     */
    public void testPropertyCompletionEndOfKey() {
        myFixture.configureByFiles(getFileName("component-options-filtered"));
        myFixture.completeBasic();
        myFixture.type('\n');
        myFixture.type('x');
        myFixture.checkResultByFile(getFileName("component-options-filtered-result"));
    }

    private String getFileName(String fileNamePrefix) {
        if (CamelPreferenceService.getService().getCamelCatalogProvider() == CamelCatalogProvider.AUTO) {
            CamelPreferenceService.getService().setCamelCatalogProvider(CamelCatalogProvider.QUARKUS);
        }
        return String.format("%s.yaml", fileNamePrefix);
    }
}
