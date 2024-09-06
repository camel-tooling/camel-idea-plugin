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

import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;
import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Testing smart completion with Camel Java DSL
 */
public class JavaEndpointSmartCompletionValueTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testEnumValue() {
        myFixture.configureByFiles("CompleteJavaEndpointValueEnumTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(8, strings.size());
        assertThat(strings, Matchers.contains("file:inbox?readLock=changed", "file:inbox?readLock=fileLock", "file:inbox?readLock=idempotent",
            "file:inbox?readLock=idempotent-changed", "file:inbox?readLock=idempotent-rename", "file:inbox?readLock=markerFile",
            "file:inbox?readLock=none", "file:inbox?readLock=rename"));
    }

    public void testDefaultValue() {
        myFixture.configureByFiles("CompleteJavaEndpointValueDefaultTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(1, strings.size());
        assertThat(strings, Matchers.contains("file:inbox?delay=500"));
    }

    public void testBooleanValue() {
        myFixture.configureByFiles("CompleteJavaEndpointValueBooleanTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertThat(strings, Matchers.contains("file:inbox?delay=1000&recursive=false", "file:inbox?delay=1000&recursive=true"));
    }

    private String getJavaInTheMiddleUnresolvedValueTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?repeatCount=10&fixedRate=<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testInTheMiddleUnresolvedValuesCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaInTheMiddleUnresolvedValueTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "timer:trigger?repeatCount=10");
        assertThat(strings, Matchers.contains("timer:trigger?repeatCount=10&fixedRate=false", "timer:trigger?repeatCount=10&fixedRate=true"));
        assertEquals(2, strings.size());
    }

    private String getJavaUnresolvedValueWithPreTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"timer:trigger?exchangePattern=In<caret>&repeatCount=10\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }
    public void testUnresolvedValueWithPreTestCompletion() {
        myFixture.configureByText("JavaCaretInMiddleOptionsTestData.java", getJavaUnresolvedValueWithPreTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "timer:trigger?repeatCount=10");
        assertContainsElements(strings,
            "timer:trigger?exchangePattern=InOut",
            "timer:trigger?exchangePattern=InOnly");
        assertEquals(2, strings.size());
    }

}
