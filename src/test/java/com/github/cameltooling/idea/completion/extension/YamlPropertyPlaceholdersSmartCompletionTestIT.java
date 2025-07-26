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
 * Testing smart completion of camel property placeholders ({{...}}) inside yaml routes
 */
public class YamlPropertyPlaceholdersSmartCompletionTestIT extends AbstractPropertyPlaceholderIT {

    public void testCompletion() {
        myFixture.configureByFiles("yaml/route.yaml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("yaml/route_after.yaml",
                List.of("ftp.client", "ftp.server"),
                REPLACE_SELECT_CHAR);
    }

    public void testEmptyPlaceholder() {
        myFixture.configureByFiles("yaml/empty_placeholder.yaml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("yaml/empty_placeholder_after.yaml",
                List.of("ftp.client", "ftp.server", "ftx"),
                REPLACE_SELECT_CHAR);
    }

    public void testYamlRoutesAreNotIncludedInProperties() {
        myFixture.configureByFiles("yaml/not_offering_yaml_routes_as_properties.yaml", "routes.yaml");
        myFixture.completeBasic();

        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertFalse(strings.contains("routes.route.from.uri"));
    }

    public void testOneOpenOneClosed() {
        myFixture.configureByFiles("yaml/one_open_one_closed.yaml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("yaml/one_open_one_closed_after.yaml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionWithOpenPropertyPrefixPresent() {
        myFixture.configureByFiles("yaml/open_property_prefix_completion.yaml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("yaml/open_property_prefix_completion_after_replace.yaml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionWithOpenPropertyPrefixPresentWithoutReplacement() {
        myFixture.configureByFiles("yaml/open_property_prefix_completion.yaml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("yaml/open_property_prefix_completion_after_normal.yaml",
                List.of("ftp.client", "ftp.server"),
                Lookup.NORMAL_SELECT_CHAR);
    }

    public void testCompletionWorksInSecondProperty() {
        myFixture.configureByFiles("yaml/completion_works_in_second_property.yaml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("yaml/completion_works_in_second_property_after.yaml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCamelIsNotPresent() {
        myFixture.getProject().getService(CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("yaml/route.yaml", "CompleteJavaPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertTrue(strings.isEmpty());
    }
}
