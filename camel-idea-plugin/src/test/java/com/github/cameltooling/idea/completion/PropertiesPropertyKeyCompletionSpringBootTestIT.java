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

import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.jetbrains.annotations.Nullable;

/**
 * The integration test allowing to ensure that a specific version 3 of the catalog can be downloaded in case of
 * the Spring Boot runtime.
 */
public class PropertiesPropertyKeyCompletionSpringBootTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{
            "org.apache.camel:camel-core-engine:3.16.0",
            "org.apache.camel.springboot:camel-spring-boot:3.16.0",
            "org.apache.camel.springboot:camel-sql-starter:3.16.0"
        };
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/property";
    }

    /**
     * Ensures that group suggestions can properly be proposed.
     */
    public void testGroupSuggestion() {
        myFixture.configureByFiles(getFileName("full-first-key-with-separator"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "camel.main.");
        assertContainsElements(strings, "camel.springboot.", "camel.component.", "camel.language.", "camel.dataformat.");
    }

    /**
     * Ensures that main option suggestions can properly be proposed.
     */
    public void testMainOptionSuggestionNonFiltered() {
        myFixture.configureByFiles(getFileName("springboot-options"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.springboot.debugging = ", "camel.springboot.duration-max-seconds = ", "camel.springboot.auto-startup = ");
    }

    /**
     * Ensures that component name suggestions can properly be proposed.
     */
    public void testComponentNameSuggestion() {
        myFixture.configureByFiles(getFileName("component-names-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.component.ftp.", "camel.component.bean.", "camel.component.cql.");
    }

    /**
     * Ensures that component option suggestions can properly be proposed.
     */
    public void testComponentOptionSuggestion() {
        myFixture.configureByFiles(getFileName("component-options"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertComponentOptionSuggestion(strings);
    }

    protected void assertComponentOptionSuggestion(List<String> strings) {
        assertContainsElements(strings, "camel.component.sql.lazy-start-producer = ", "camel.component.sql.use-placeholder = ",
            "camel.component.sql.bridge-error-handler = ", "camel.component.sql.enabled = ");
    }

    /**
     * Ensures that language name suggestions can properly be proposed.
     */
    public void testLanguageNameSuggestion() {
        myFixture.configureByFiles(getFileName("language-names"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, "camel.language.jsonpath.", "camel.language.bean.", "camel.language.xpath.");
    }

    /**
     * Ensures that data format name suggestions can properly be proposed.
     */
    public void testDataFormatNameSuggestion() {
        myFixture.configureByFiles(getFileName("data-format-names-non-filtered"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDataFormatNameSuggestion(strings);
    }

    protected void assertDataFormatNameSuggestion(List<String> strings) {
        assertContainsElements(strings, "camel.dataformat.jackson.", "camel.dataformat.csv.", "camel.dataformat.bindyCsv.");
    }

    protected String getFileName(String fileNamePrefix) {
        return String.format("%s.properties", fileNamePrefix);
    }
}
