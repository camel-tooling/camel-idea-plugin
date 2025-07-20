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
package com.github.cameltooling.idea.completion.extension;

import java.util.Collections;
import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.codeInsight.completion.CompletionType;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.lookup.Lookup;

/**
 * Testing smart completion of camel property placeholders ({{...}}) inside Java classes
 */
public class JavaPropertyPlaceholdersSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final List<String> INCLUDED_PROPERTIES = List.of("ftp.client", "ftp.server", "ftx");
    public static final int PROP_COUNT_WITH_EXCLUDED_PROPS = 10;

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

    public void testCompletion() {
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, INCLUDED_PROPERTIES);

        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResultByFile("CompletePropertyPlaceholderTestData_after.java");
    }

    public void testCamelIsNotPresent() {
        myFixture.getProject().getService(CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue(strings.isEmpty());
    }

    public void testWithExcludeFile() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("**/CompleteExclude*"));
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, INCLUDED_PROPERTIES);
    }

    public void testWithExcludeFileWithPath() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("**/src/CompleteExclude*"));
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, INCLUDED_PROPERTIES);
    }

    public void testWithExcludeNoMatchFileWithPath() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("my/test/CompleteExclude"));
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(strings, INCLUDED_PROPERTIES);
        assertEquals(PROP_COUNT_WITH_EXCLUDED_PROPS, strings.size());
    }

    public void testWithExcludePath() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("**/src/*"));
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue(strings.isEmpty());
    }

    public void testWithExcludeEmptyListFile() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList(""));
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(PROP_COUNT_WITH_EXCLUDED_PROPS, strings.size());
    }

    public void testWithExcludeSpaceListFile() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList(" "));
        myFixture.configureByFiles("CompletePropertyPlaceholderTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(PROP_COUNT_WITH_EXCLUDED_PROPS, strings.size());
    }

    public void testPrefixAndSuffixAroundCaret() {
        myFixture.testCompletionTyping("PrefixAndSuffixAroundCaret.java", "ftp\t", "PrefixAndSuffixAroundCaret_after.java", "CompleteJavaPropertyTestData.properties");
    }

    public void testTextAfterPlaceholderNotDeleted() {
        myFixture.testCompletionTyping("TextAfterPlaceholderNotDeleted.java", "ftp\t", "TextAfterPlaceholderNotDeleted_after.java", "CompleteJavaPropertyTestData.properties");
    }

    public void testCompletionWithPropertyPrefixPresent() {
        myFixture.configureByFiles("CompletionWithPropertyPrefixPresent.java", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("CompletionWithPropertyPrefixPresent_after.java",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionWithOpenPropertyPrefixPresent() {
        myFixture.configureByFiles("CompletionWithOpenPropertyPrefixPresent.java", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("CompletionWithOpenPropertyPrefixPresent_after_replace.java",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionWithOpenPropertyPrefixPresentWithoutReplacement() {
        myFixture.configureByFiles("CompletionWithOpenPropertyPrefixPresent.java", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("CompletionWithOpenPropertyPrefixPresent_after_normal.java",
                List.of("ftp.client", "ftp.server"),
                Lookup.NORMAL_SELECT_CHAR);
    }

    public void testCompletionDoesNotReplaceNextPlaceholder() {
        myFixture.configureByFiles("CompletionDoesNotReplaceNextPlaceholder.java", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("CompletionDoesNotReplaceNextPlaceholder_after.java",
                INCLUDED_PROPERTIES);
    }

    private void runCompletionTest(String expectedFile, List<String> expectedLookupElements) {
        runCompletionTest(expectedFile, expectedLookupElements, Lookup.REPLACE_SELECT_CHAR);
    }

    private void runCompletionTest(String expectedFile, List<String> expectedLookupElements, char lookupChar) {
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, expectedLookupElements);
        myFixture.type(lookupChar);
        myFixture.checkResultByFile(expectedFile);
    }

}
