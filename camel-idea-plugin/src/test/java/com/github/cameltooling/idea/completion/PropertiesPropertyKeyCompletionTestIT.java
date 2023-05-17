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
import com.github.cameltooling.idea.service.CamelProjectPreferenceService;
import com.intellij.codeInsight.lookup.LookupElement;

/**
 * Testing the completion of the property keys based on the options defined in the metadata of component, data format,
 * language and main in properties files.
 */
public class PropertiesPropertyKeyCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/property";
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CamelProjectPreferenceService.getService(getProject()).setCamelCatalogProvider(null);
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
        testSuggestionWhenEmptyKey("partial-first-key");
    }

    /**
     * Ensures that group suggestions are properly filtered when the first key section is fully provided but without
     * the separator.
     */
    public void testGroupSuggestionWithFullFirstKeyWithoutSeparator() {
        testSuggestionWhenEmptyKey("full-first-key-without-separator");
    }

    /**
     * Ensures that group suggestions are properly filtered when the first key section is fully provided but with
     * the separator.
     */
    public void testGroupSuggestionWithFullFirstKeyWithSeparator() {
        testSuggestionWhenEmptyKey("full-first-key-with-separator");
    }

    /**
     * Ensures that group suggestions for default Camel Runtime matches with the expectations.
     */
    public void testGroupSuggestionForDefaultCamelRuntime() {
        CamelProjectPreferenceService.getService(getProject()).setCamelCatalogProvider(CamelCatalogProvider.DEFAULT);
        testGroupSuggestionWithFullFirstKeyWithSeparator();
    }

    /**
     * Ensures that group suggestions for Quarkus Camel Runtime matches with the expectations.
     */
    public void testGroupSuggestionForQuarkusCamelRuntime() {
        CamelProjectPreferenceService.getService(getProject()).setCamelCatalogProvider(CamelCatalogProvider.QUARKUS);
        testGroupSuggestionWithFullFirstKeyWithSeparator();
    }

    /**
     * Ensures that group suggestions for Karaf Camel Runtime matches with the expectations.
     */
    public void testGroupSuggestionForKarafCamelRuntime() {
        CamelProjectPreferenceService.getService(getProject()).setCamelCatalogProvider(CamelCatalogProvider.KARAF);
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "camel.main.");
        assertContainsElements(strings, "camel.component.", "camel.language.", "camel.dataformat.");
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
        assertContainsElements(strings, "camel.main.");
        assertDoesntContain(strings, "camel.component.", "camel.language.", "camel.dataformat.");
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
        assertContainsElements(strings, "camel.main.debugging = ", "camel.main.configurations = ", "camel.main.auto-startup = ");
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
        myFixture.configureByFiles(getFileName("main-options-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.main.debugging = ");
        assertDoesntContain(strings, "camel.main.configurations = ", "camel.main.auto-startup = ");
    }

    /**
     * Ensures that component name suggestions can properly be proposed non filtered.
     */
    public void testComponentNameSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("component-names-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.component.ftp.", "camel.component.bean.", "camel.component.cql.");
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
        assertContainsElements(strings, "camel.component.ftp.");
        assertDoesntContain(strings, "camel.component.bean.", "camel.component.cql.");
    }

    /**
     * Ensures that component option suggestions can properly be proposed non filtered.
     */
    public void testComponentOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("component-options-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(
            strings, "camel.component.quartz.scheduler = ", "camel.component.quartz.enable-jmx = ",
            "camel.component.quartz.properties = "
        );
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
        assertContainsElements(strings, "camel.component.quartz.scheduler = ");
        assertDoesntContain(strings, "camel.component.quartz.enable-jmx = ", "camel.component.quartz.properties = ");
    }

    /**
     * Ensures that language name suggestions can properly be proposed non filtered.
     */
    public void testLanguageNameSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("language-names"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.language.jsonpath.", "camel.language.bean.", "camel.language.xpath.");
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
        assertContainsElements(strings, "camel.language.bean.");
        assertDoesntContain(strings, "camel.language.jsonpath.", "camel.language.xpath.");
    }

    /**
     * Ensures that language option suggestions can properly be proposed non filtered.
     */
    public void testLanguageOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("language-options"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.language.bean.method = ", "camel.language.bean.scope = ", "camel.language.bean.bean-type = ");
        assertDoesntContain(strings, "camel.language.bean.id = ");
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
        assertContainsElements(strings, "camel.language.bean.scope = ");
        assertDoesntContain(strings, "camel.language.bean.method = ", "camel.language.bean.bean-type = ");
    }

    /**
     * Ensures that data format name suggestions can properly be proposed non filtered.
     */
    public void testDataFormatNameSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("data-format-names-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.dataformat.jackson.", "camel.dataformat.csv.", "camel.dataformat.bindyCsv.");
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
        myFixture.configureByFiles(getFileName("data-format-names-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.dataformat.csv.");
        assertDoesntContain(strings, "camel.dataformat.jackson.", "camel.dataformat.xpath.");
    }

    /**
     * Ensures that data format option suggestions can properly be proposed non filtered.
     */
    public void testDataFormatOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("data-format-options"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(
            strings, "camel.dataformat.jackson.include = ", "camel.dataformat.jackson.pretty-print = ",
            "camel.dataformat.jackson.json-view = "
        );
        assertDoesntContain(strings, "camel.dataformat.jackson.id = ");
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
        assertContainsElements(strings, "camel.dataformat.jackson.include = ");
        assertDoesntContain(strings, "camel.dataformat.jackson.pretty-print = ", "camel.dataformat.jackson.json-view = ");
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
        assertContainsElements(strings, "camel.dataformat.bindyCsv.allow-empty-stream = ");
        assertDoesntContain(strings, "camel.dataformat.bindyCsv.class-type = ", "camel.dataformat.bindyCsv.local = ");
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
        assertContainsElements(strings, "camel.component.aws2-athena.access-key = ");
        assertDoesntContain(strings, "camel.component.aws2-athena.query-string = ", "camel.component.aws2-athena.output-location = ");
    }

    private void testSuggestionWhenEmptyKey(String fileNamePrefix) {
        myFixture.configureByFiles(getFileName(fileNamePrefix));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.main.", "camel.component.", "camel.language.", "camel.dataformat.");
    }

    private String getFileName(String fileNamePrefix) {
        return String.format("%s.properties", fileNamePrefix);
    }
}
