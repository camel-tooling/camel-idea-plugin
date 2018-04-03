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
 * Testing smart completion with Camel Java DSL and specific the bean smart completion
 */
public class JavaCamelBeanReferenceSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/method";
    }

    private static final String CAMEL_ROUTE_WITH_INCORRECT_BEAN_REF =
        "import org.apache.camel.builder.RouteBuilder;\n"
            + "import org.apache.camel.main.Main;\n"
            + "\n"
            + "public final class CompleteJavaBeanTestData extends RouteBuilder {\n"
            + "\n"
            + "    @Override\n"
            + "    public void configure() {\n"
            + "        from(\"file:inbox\")\n"
            + "            .bean(MyJavaBeanTestData.class, \"<caret>\")\n"
            + "            .to(\"log:out\");\n"
            + "    }\n"
            + "   private void myJavaBeanTestData() { }\n"
            + "   public void letsDoThis() { }\n"
            + "   private void thisIsVeryPrivate() {}\n"
            + "}";

    private static final String CAMEL_ROUTE_WITH_INTERNAL_BEAN_REF =
        "import org.apache.camel.builder.RouteBuilder;\n"
            + "import org.apache.camel.main.Main;\n"
            + "\n"
            + "public final class CompleteJavaBeanTestData extends RouteBuilder {\n"
            + "\n"
            + "    @Override\n"
            + "    public void configure() {\n"
            + "        from(\"file:inbox\")\n"
            + "            .bean(CompleteJavaBeanTestData.class, \"<caret>\")\n"
            + "            .to(\"log:out\");\n"
            + "    }\n"
            + "   public void letsDoThis() { }\n"
            + "   private void thisIsVeryPrivate() {}\n"
            + "}";

    private static final String CAMEL_ROUTE_WITH_WHEN_METHOD_REF =
        "import org.apache.camel.builder.RouteBuilder;\n"
        + "import org.apache.camel.main.Main;\n"
        + "\n"
        + "public final class CompleteJavaBeanTestData extends RouteBuilder {\n"
        + "\n"
        + "    @Override\n"
        + "    public void configure() {\n"
        + "       from(\"direct:start\")\n"
        + "         .choice().when().method(CompleteJavaBeanTestData.class,\"<caret>\");\n"
        + "    }\n"
        + "   public void letsDoThis() { }\n"
        + "   private void thisIsVeryPrivate() {}\n"
        + "}";

    private static final String CAMEL_ROUTE_CARET_OUTSIDE_BEAN_REF =
        "import org.apache.camel.builder.RouteBuilder;\n"
            + "import org.apache.camel.main.Main;\n"
            + "\n"
            + "public final class CompleteJavaBeanTestData extends RouteBuilder {\n"
            + "\n"
            + "    @Override\n"
            + "    public void configure() {\n"
            + "        from(\"file:inbox\")\n"
            + "            .<caret>bean(CompleteJavaBeanTestData.class, \"\")\n"
            + "            .to(\"log:out\");\n"
            + "    }\n"
            + "   public void letsDoThis() { }\n"
            + "   private void thisIsVeryPrivate() {}\n"
            + "}";
    private static final String CAMEL_ROUTE_CARET_INSIDE_PROCESS_TAG =
        "import org.apache.camel.builder.RouteBuilder;\n"
            + "import org.apache.camel.main.Main;\n"
            + "\n"
            + "public final class CompleteJavaBeanTestData extends RouteBuilder {\n"
            + "\n"
            + "    @Override\n"
            + "    public void configure() {\n"
            + "        from(\"file:inbox\")\n"
            + "            .process(exchange -> {<caret>})\n"
            + "            .bean(CompleteJavaBeanTestData.class, \"\")\n"
            + "            .to(\"log:out\");\n"
            + "    }\n"
            + "   public void letsDoThis() { }\n"
            + "   private void thisIsVeryPrivate() {}\n"
            + "}";

    public void testJavaBeanTestDataCompletionWithIncorrectBeanRef() {
        myFixture.configureByText("CompleteJavaBeanTestData.java", CAMEL_ROUTE_WITH_INCORRECT_BEAN_REF);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate", "myJavaBeanTestData")));
    }

    public void testJavaBeanTestDataCompletion() {
        myFixture.configureByText("CompleteJavaBeanTestData.java", CAMEL_ROUTE_WITH_INTERNAL_BEAN_REF);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate")));
        assertThat(strings, Matchers.hasItems("letsDoThis"));
    }

    public void testJavaBeanTestDataCompletionWithWhenMethod() {
        myFixture.configureByText("CompleteJavaBeanTestData.java", CAMEL_ROUTE_WITH_WHEN_METHOD_REF);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate")));
        assertThat(strings, Matchers.hasItems("letsDoThis"));
    }

    public void testJavaBeanTestDataCompletionWithCaretOutSideBeanRef() {
        myFixture.configureByText("CompleteJavaBeanTestData.java", CAMEL_ROUTE_CARET_OUTSIDE_BEAN_REF);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate")));
    }

    public void testJavaBeanTestDataCompletionWithCaretInsideProcessTag() {
        myFixture.configureByText("CompleteJavaBeanTestData.java", CAMEL_ROUTE_CARET_INSIDE_PROCESS_TAG);
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate")));
    }

    public void testJavaBeanTestDataCompletionWithCaretInsideMultipleMethodRef() {
        myFixture.configureByFiles("CompleteJavaBeanRoute5TestData.java", "CompleteJavaBeanMultipleMethodTestData.java",
            "CompleteJavaBeanSuperClassTestData.java", "CompleteJavaBeanMethodPropertyTestData.properties");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(3, strings.size());
        assertThat(strings, Matchers.hasItems("multipleMethodsWithAnotherName", "multipleMethodsWithSameName", "multipleMethodsWithSameName"));
    }

    public void testJavaBeanWithClassHierarchy() {
        myFixture.configureByFiles("CompleteJavaBeanRouteTestData.java", "CompleteJavaBeanTestData.java", "CompleteJavaBeanSuperClassTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate")));
        assertThat(strings, Matchers.hasItems("letsDoThis", "anotherBeanMethod", "mySuperAbstractMethod", "mySuperMethod", "myOverLoadedBean", "myOverLoadedBean"));
        assertEquals("There is many options", 6, strings.size());
    }

    public void testJavaBeanTestDataCompletion2File() {
        myFixture.configureByFiles("CompleteJavaBeanRoute2TestData.java", "CompleteJavaBeanTestData.java", "CompleteJavaBeanSuperClassTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        myFixture.checkResultByFile("CompleteJavaBeanRoute2ResultData.java", true);
    }
    
    public void testJavaFieldBeanReference() {
        myFixture.configureByFiles("CompleteJavaBeanRoute1TestData.java", "CompleteJavaBeanTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertThat(strings, Matchers.not(Matchers.contains("thisIsVeryPrivate")));
        assertThat(strings, Matchers.hasItems("letsDoThis", "anotherBeanMethod", "mySuperAbstractMethod", "myOverLoadedBean", "myOverLoadedBean"));
        assertEquals("There is many options", 5, strings.size());
    }

    public void testJavaFieldBeanWithNoReference() {
        myFixture.configureByFiles("CompleteJavaBeanRoute5TestData.java", "CompleteJavaBeanTestData.java");
        myFixture.complete(CompletionType.BASIC, 1);
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals("There is many options", 0, strings.size());
    }


}
