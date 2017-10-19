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
package org.apache.camel.idea.preference.properties;

import java.util.Arrays;
import java.util.List;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;


public class CamelIgnoreAndExcludePageTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private CamelIgnoreAndExcludePage ignoreAndExcludePage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ignoreAndExcludePage = new CamelIgnoreAndExcludePage();
        ignoreAndExcludePage.createComponent();
        super.initCamelPreferencesService();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ignoreAndExcludePage = null;
        super.initCamelPreferencesService();
    }

    public void testShouldContainIgnorePropertyTable() {
        List<String> expectedIgnoredProperties = Arrays.asList("java.", "Logger.", "logger", "appender.", "rootLogger.",
                "camel.springboot.", "camel.component.", "camel.dataformat.", "camel.language.");
        ignoreAndExcludePage.reset();
        assertEquals(expectedIgnoredProperties, ignoreAndExcludePage.getMyIgnoredProperties());
        assertEquals(expectedIgnoredProperties, ignoreAndExcludePage.getIgnorePropertyFilePanel().getData());
    }

    public void testShouldContainExcludePropertyTable() {
        List<String> expectedExcludedProperties = Arrays.asList("**/log4j.properties", "**/log4j2.properties", "**/logging.properties");
        ignoreAndExcludePage.reset();
        assertEquals(expectedExcludedProperties, ignoreAndExcludePage.getMyExcludedProperties());
        assertEquals(expectedExcludedProperties, ignoreAndExcludePage.getExcludePropertyFilePanel().getData());
    }
}