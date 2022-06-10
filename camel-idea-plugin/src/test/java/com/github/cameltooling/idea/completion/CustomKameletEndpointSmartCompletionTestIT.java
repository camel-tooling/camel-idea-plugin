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

import java.io.File;
import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.completion.JavaEndpointSmartCompletionTestIT.getJavaSourceKameletSuggestionsData;

/**
 * Testing smart completion with Kamelet endpoint in a project with custom Kamelets defined in maven jar and with a
 * specific catalog.
 */
public class CustomKameletEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected void loadDependencies(@NotNull ModifiableRootModel model) {
        super.loadDependencies(model);
        File rootFolder = new File("src/test/resources/kamelets-with-jar-catalog/");
        PsiTestUtil.addLibrary(model, "com.foo:custom-kamelets:1.0", rootFolder.getPath(), "custom-kamelets.jar");
        PsiTestUtil.addLibrary(model, "org.apache.camel.kamelets:camel-kamelets:0-SNAPSHOT", rootFolder.getPath(), "specific-camel-kamelets.jar");
    }

    /**
     * Ensure that the name of the custom Kamelets can be suggested
     */
    public void testKameletSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:webhook-source");
        assertContainsElements(strings, "kamelet:chuck-norris-in-jar-source", "kamelet:ftp-source");
    }
}
