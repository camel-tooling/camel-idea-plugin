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
