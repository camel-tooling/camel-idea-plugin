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
 * Testing smart completion with Kamelet endpoint in a project with custom Kamelets defined in a custom jar.
 */
public class SimpleProjectKameletEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected void loadDependencies(@NotNull ModifiableRootModel model) {
        super.loadDependencies(model);
        File artifact = new File("src/test/resources/kamelets-with-custom-jar/custom-kamelets.jar");
        PsiTestUtil.addLibrary(model, "foo/custom-kamelets.jar", artifact.getParent(), artifact.getName());
    }

    /**
     * Ensure that the name of the custom Kamelets can be suggested
     */
    public void testKameletSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(
            strings, "kamelet:chuck-norris-in-custom-jar-source", "kamelet:ftp-source", "kamelet:webhook-source"
        );
    }

    private String getJavaKameletOptionValueSuggestionsData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"kamelet:chuck-norris-in-custom-jar-source?periodMillis=<caret>\");\n"
            + "        }\n"
            + "    }";
    }

    /**
     * Ensure that the values of a configuration option of a given Kamelet can be suggested
     */
    public void testJavaKameletOptionValueSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaKameletOptionValueSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(
            strings,
            "kamelet:chuck-norris-in-custom-jar-source?periodMillis=1000",
            "kamelet:chuck-norris-in-custom-jar-source?periodMillis=10000",
            "kamelet:chuck-norris-in-custom-jar-source?periodMillis=100000"
        );
        myFixture.type('\n');
        String javaMarkTestData = getJavaKameletOptionValueSuggestionsData().replace("<caret>", "1000");
        myFixture.checkResult(javaMarkTestData);
    }
}
