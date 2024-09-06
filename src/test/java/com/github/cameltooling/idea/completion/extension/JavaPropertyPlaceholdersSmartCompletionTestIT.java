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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.codeInsight.completion.CompletionType;
import com.github.cameltooling.idea.service.CamelPreferenceService;

/**
 * Testing smart completion with YML property classes
 */
public class JavaPropertyPlaceholdersSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

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

    public void testCompletion() {
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(2, strings.size());
    }

    public void testCamelIsNotPresent() {
        myFixture.getProject().getService(CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(0, strings.size());
    }

    public void testWithExcludeFile() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("**/CompleteExclude*"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(2, strings.size());
    }

    public void testWithExcludeFileWithPath() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("**/src/CompleteExclude*"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(2, strings.size());
    }

    public void testWithExcludeNoMatchFileWithPath() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("my/test/CompleteExclude"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(9, strings.size());
    }

    public void testWithExcludePath() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList("**/src/*"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(0, strings.size());
    }

    public void testWithExcludeEmptyListFile() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList(""));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(9, strings.size());
    }

    public void testWithExcludeSpaceListFile() {
        CamelPreferenceService.getService().setExcludePropertyFiles(Collections.singletonList(" "));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(9, strings.size());
    }
}
