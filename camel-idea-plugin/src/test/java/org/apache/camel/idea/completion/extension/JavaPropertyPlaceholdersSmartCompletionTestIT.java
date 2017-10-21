/**
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
package org.apache.camel.idea.completion.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.components.ServiceManager;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.service.CamelService;

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
        ServiceManager.getService(myFixture.getProject(), CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(0, strings.size());
    }

    public void testWithExcludeFile() {
        ServiceManager.getService(CamelPreferenceService.class).setExcludePropertyFiles(Collections.singletonList("**/CompleteExclude*"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(2, strings.size());
    }

    public void testWithExcludeFileWithPath() {
        ServiceManager.getService(CamelPreferenceService.class).setExcludePropertyFiles(Collections.singletonList("**/src/CompleteExclude*"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(2, strings.size());
    }

    public void testWithExcludeNoMatchFileWithPath() {
        ServiceManager.getService(CamelPreferenceService.class).setExcludePropertyFiles(Collections.singletonList("my/test/CompleteExclude"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("ftp.client}}", "ftp.server}}")));
        assertEquals(9, strings.size());
    }

    public void testWithExcludePath() {
        ServiceManager.getService(CamelPreferenceService.class).setExcludePropertyFiles(Collections.singletonList("**/src/*"));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(0, strings.size());
    }

    public void testWithExcludeEmptyListFile() {
        ServiceManager.getService(CamelPreferenceService.class).setExcludePropertyFiles(Collections.singletonList(""));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(9, strings.size());
    }

    public void testWithExcludeSpaceListFile() {
        ServiceManager.getService(CamelPreferenceService.class).setExcludePropertyFiles(Collections.singletonList(" "));
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteJavaPropertyTestData.properties", "CompleteExcludePropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(9, strings.size());
    }
}