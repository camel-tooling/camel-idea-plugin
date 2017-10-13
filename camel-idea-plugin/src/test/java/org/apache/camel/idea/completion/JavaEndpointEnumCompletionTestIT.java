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
package org.apache.camel.idea.completion;

import java.util.List;
import com.intellij.codeInsight.completion.CompletionType;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;

/**
 * Testing smart completion with Camel Java DSL
 */
public class JavaEndpointEnumCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testEnum() {
        myFixture.configureByFiles("CompleteJavaEndpointSyntaxEnumTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(5, strings.size());
        assertThat(strings, Matchers.contains("jms:", "jms:queue", "jms:topic", "jms:temp-queue", "jms:temp-topic"));
    }

    private String getJavaCaretAfterColonWithPreDataEnumTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"jms:qu<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }

    public void testJavaCaretAfterColonWithPreDataEnum() {
        myFixture.configureByText("CompleteJavaEndpointSyntaxEnumTestData.java", getJavaCaretAfterColonWithPreDataEnumTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(3, strings.size());
        assertThat(strings, Matchers.containsInAnyOrder("jms:qu", "jms:queue", "jms:temp-queue"));
    }

    private String getJavaCaretInMiddleOfWithEnumTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"jms:temp<caret>topic\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }

    public void testJavaCaretInMiddleOfWithDataEnum() {
        myFixture.configureByText("CompleteJavaEndpointSyntaxEnumTestData.java", getJavaCaretInMiddleOfWithEnumTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(3, strings.size());
        assertThat(strings, Matchers.containsInAnyOrder("jms:temp", "jms:temp-queue", "jms:temp-topic"));
    }

    private String getJavaCaretAfterColonTestData() {
        return "import org.apache.camel.builder.RouteBuilder;\n"
            + "public class MyRouteBuilder extends RouteBuilder {\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"jms:temp-<caret>\")\n"
            + "                .to(\"file:outbox?delete=true&fileExist=Append\");\n"
            + "        }\n"
            + "    }";
    }

    public void testJavaCaretAfterColonWithEnum() {
        myFixture.configureByText("CompleteJavaEndpointSyntaxEnumTestData.java", getJavaCaretAfterColonTestData());
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(3, strings.size());
        assertThat(strings, Matchers.containsInAnyOrder("jms:temp-", "jms:temp-queue", "jms:temp-topic"));
    }

}