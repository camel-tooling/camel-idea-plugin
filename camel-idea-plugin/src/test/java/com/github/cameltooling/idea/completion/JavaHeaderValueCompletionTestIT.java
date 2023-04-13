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
import org.jetbrains.annotations.Nullable;

/**
 * Testing the completion of the header values based on the headers defined in the metadata of a component.
 */
public class JavaHeaderValueCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String CAMEL_CORE_MODEL_MAVEN_ARTIFACT = String.format("org.apache.camel:camel-core-model:%s", CAMEL_VERSION);
    private static final String CAMEL_ATHENA_MAVEN_ARTIFACT = String.format("org.apache.camel:camel-aws2-athena:%s", CAMEL_VERSION);

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_ATHENA_MAVEN_ARTIFACT};
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/header/set";
    }

    /**
     * Ensures that suggestions of values of header of type enum can be found.
     */
    public void testEnumSuggestions() {
        for (TestType type : TestType.values()) {
            testEnumSuggestions(type);
        }
    }
    private void testEnumSuggestions(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueEnumSuggestions"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'getQueryExecution'", strings.stream().anyMatch(s -> s.contains("getQueryExecution")));
        assertTrue("Should contain 'listQueryExecutions'", strings.stream().anyMatch(s -> s.contains("listQueryExecutions")));
    }

    /**
     * Ensures that enum suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testEnumSuggestionsInstancesOfOptionSuggestion() {
        for (TestType type : TestType.values()) {
            testEnumSuggestionsInstancesOfOptionSuggestion(type);
        }
    }
    private void testEnumSuggestionsInstancesOfOptionSuggestion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueEnumSuggestions"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions)
                .map(LookupElement::getObject)
                .anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that suggestions of values of header of type boolean can be found.
     */
    public void testBooleanSuggestions() {
        for (TestType type : TestType.values()) {
            testBooleanSuggestions(type);
        }
    }
    private void testBooleanSuggestions(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueBooleanSuggestions"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'true'", strings.stream().anyMatch(s -> s.contains("true")));
        assertTrue("Should contain 'false'", strings.stream().anyMatch(s -> s.contains("false")));
    }

    /**
     * Ensures that boolean suggestions are only instances of {@link OptionSuggestion}.
     */
    public void testBooleanSuggestionsInstancesOfOptionSuggestion() {
        for (TestType type : TestType.values()) {
            testBooleanSuggestionsInstancesOfOptionSuggestion(type);
        }
    }
    private void testBooleanSuggestionsInstancesOfOptionSuggestion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueBooleanSuggestions"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions)
                .map(LookupElement::getObject)
                .anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that suggestions of values of header with default value can be found.
     */
    public void testDefaultValueSuggestion() {
        for (TestType type : TestType.values()) {
            testDefaultValueSuggestion(type);
        }
    }
    private void testDefaultValueSuggestion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueDefaultSuggestion"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain '/'", strings.stream().anyMatch(s -> s.contains("/")));
    }

    /**
     * Ensures that the suggestion of the default value is an instance of {@link OptionSuggestion}.
     */
    public void testDefaultValueSuggestionInstanceOfOptionSuggestion() {
        for (TestType type : TestType.values()) {
            testDefaultValueSuggestionInstanceOfOptionSuggestion(type);
        }
    }
    private void testDefaultValueSuggestionInstanceOfOptionSuggestion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueDefaultSuggestion"));
        myFixture.completeBasic();
        LookupElement[] suggestions = myFixture.getLookupElements();
        assertNotNull(suggestions);
        assertTrue(
            "Only instances of OptionSuggestion are expected",
            Arrays.stream(suggestions)
                .map(LookupElement::getObject)
                .anyMatch(o -> o instanceof OptionSuggestion)
        );
    }

    /**
     * Ensures that the completion of values of header of type enum works as expected.
     */
    public void testEnumCompletion() {
        for (TestType type : TestType.values()) {
            testEnumCompletion(type);
        }
    }
    private void testEnumCompletion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueEnumSuggestions"));
        myFixture.completeBasic();
        myFixture.type("getQuery");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'getQueryExecution'", strings.stream().anyMatch(s -> s.contains("getQueryExecution")));
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderValueEnumSuggestionsResult"));
    }

    /**
     * Ensures that the completion of values of header of type boolean works as expected.
     */
    public void testBooleanCompletion() {
        for (TestType type : TestType.values()) {
            testBooleanCompletion(type);
        }
    }
    private void testBooleanCompletion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueBooleanSuggestions"));
        myFixture.completeBasic();
        myFixture.type("tru");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'true'", strings.stream().anyMatch(s -> s.contains("true")));
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderValueBooleanSuggestionsResult"));
    }

    /**
     * Ensures that the completion of values of header with default value works as expected.
     */
    public void testDefaultValueCompletion() {
        for (TestType type : TestType.values()) {
            testDefaultValueCompletion(type);
        }
    }
    private void testDefaultValueCompletion(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueDefaultSuggestion"));
        myFixture.completeBasic();
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderValueDefaultSuggestionResult"));
    }

    /**
     * Ensures that the completion of values of header of type enum works as expected even
     * with an unknown component.
     */
    public void testEnumCompletionWithUnknownComponent() {
        for (TestType type : TestType.values()) {
            testEnumCompletionWithUnknownComponent(type);
        }
    }

    private void testEnumCompletionWithUnknownComponent(TestType type) {
        myFixture.configureByFiles(type.getFilePath("HeaderValueEnumSuggestionsUnknownComponent"));
        myFixture.completeBasic();
        myFixture.type("crea");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'CREATE'", strings.stream().anyMatch(s -> s.contains("CREATE")));
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderValueEnumSuggestionsUnknownComponentResult"));
    }

    /**
     * Ensures that the completion of values of header of type enum works as expected even
     * with a fully qualified constant as name.
     */
    public void testEnumCompletionWithFullyQualifiedConstantAsName() {
        TestType type = TestType.JAVA;
        myFixture.configureByFiles(type.getFilePath("HeaderValueEnumSuggestionsWithFullyQualifiedConstantAsName"));
        myFixture.completeBasic();
        myFixture.type("crea");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'CREATE'", strings.stream().anyMatch(s -> s.contains("CREATE")));
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderValueEnumSuggestionsWithFullyQualifiedConstantAsNameResult"));
    }

    /**
     * Ensures that the completion of values of header of type enum works as expected even
     * if with a non fully qualified constant as name.
     */
    public void testEnumCompletionWithNonFullyQualifiedConstantAsName() {
        TestType type = TestType.JAVA;
        myFixture.configureByFiles(type.getFilePath("HeaderValueEnumSuggestionsWithNonFullyQualifiedConstantAsName"));
        myFixture.completeBasic();
        myFixture.type("getQuery");
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue("Should contain 'getQueryExecution'", strings.stream().anyMatch(s -> s.contains("getQueryExecution")));
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderValueEnumSuggestionsWithNonFullyQualifiedConstantAsNameResult"));
    }

    enum TestType {
        JAVA,
        XML,
        YAML;

        public String getFilePath(String fileName) {
            return String.format("%s.%s", fileName, name().toLowerCase());
        }
    }
}
