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
import java.util.List;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.components.ServiceManager;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.service.CamelService;

/**
 * Testing smart completion with YML property classes
 */
public class YamlPropertyPlaceholdersSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testCompletion() {
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertTrue(strings.containsAll(Arrays.asList("example.generateOrderPeriod}}", "example.processOrderPeriod}}",
            "mysql.service.database}}", "mysql.service.host}}",
            "mysql.service.port}}", "spring.datasource.password}}",
            "spring.datasource.url}}", "spring.datasource.username}}",
            "spring.jpa.hibernate.ddl-auto}}", "spring.jpa.show-sql}}")));
        assertEquals(10, strings.size());
    }

    public void testCamelIsNotPresent() {
        ServiceManager.getService(myFixture.getProject(), CamelService.class).setCamelPresent(false);
        myFixture.configureByFiles("CompleteYmlPropertyTestData.java", "CompleteYmlPropertyTestData.yml");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(0, strings.size());
    }
}