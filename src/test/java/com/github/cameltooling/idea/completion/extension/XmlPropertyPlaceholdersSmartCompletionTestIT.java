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

import java.util.List;

import com.github.cameltooling.idea.service.CamelService;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;

import static com.intellij.codeInsight.lookup.Lookup.REPLACE_SELECT_CHAR;

/**
 * Testing smart completion of camel property placeholders ({{...}}) inside xml routes
 */
public class XmlPropertyPlaceholdersSmartCompletionTestIT extends AbstractPropertyPlaceholderIT {

    public void testCompletion() {
        myFixture.configureByFiles("xml/route.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/route_after.xml",
                List.of("ftp.client", "ftp.server"),
                REPLACE_SELECT_CHAR,
                "ftp.client");
    }

    public void testTwoPlaceholdersInSingleString() {
        myFixture.configureByFiles("xml/two_placeholders_in_single_string.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/two_placeholders_in_single_string_after.xml",
                List.of("ftp.client", "ftp.server", "ftx"));
    }

    public void testCompletionWithPropertyPrefixPresent() {
        myFixture.configureByFiles("xml/property_prefix_completion.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/property_prefix_completion_after.xml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionWithOpenPropertyPrefixPresent() {
        myFixture.configureByFiles("xml/open_property_prefix_completion.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/open_property_prefix_completion_after.xml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionWithOpenPropertyPrefixPresentWithoutReplacement() {
        myFixture.configureByFiles("xml/open_property_prefix_completion.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/open_property_prefix_completion_after.xml",
                List.of("ftp.client", "ftp.server"),
                Lookup.NORMAL_SELECT_CHAR);
    }

    public void testCompletionDoesNotReplaceNextPlaceholder() {
        myFixture.configureByFiles("xml/completion_does_not_replace_next_placeholder.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/completion_does_not_replace_next_placeholder_after.xml",
                List.of("ftp.client", "ftp.server", "ftx"));
    }

    public void testSpringXml() {
        myFixture.configureByFiles("xml/spring.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/spring_after.xml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCamelIsNotPresent() {
        myFixture.getProject().getService(CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("xml/route.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue(strings.isEmpty());
    }

}

