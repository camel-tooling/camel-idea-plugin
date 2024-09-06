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
package com.github.cameltooling.idea.util;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;

public class IdeaUtilsIsCamelRouteStartExtendedTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String CODE = "import org.apache.camel.builder.RouteBuilder;\n"
        + "\n"
        + "public abstract class MyRouteBuilder extends RouteBuilder{\n"
        + "\n"
        + "\n  private String log = \"log:out\"\n"
        + "\n"
        + "    public class MySpecialRoute extends MyRouteBuilder{\n"
        + "\n"
        + "        @Override\n"
        + "        public void configure() throws Exception {\n"
        + "            from(\"file:inbox\")\n"
        + "                .to(log);\n"
        + "        }\n"
        + "    }\n"
        + "\n"
        + "}\n";

    public void testStartRoute() {
        // caret is at start of rout in the test java file
        myFixture.configureByText("DummyTestData.java", CODE);

        // this does not work with <caret> in the source code
        // PsiElement element = myFixture.getElementAtCaret();
        PsiElement element = myFixture.findElementByText("\"file:inbox\"", PsiLiteralExpression.class);

        assertTrue(CamelIdeaUtils.getService().isCamelRouteStart(element));
    }

    public void testNotStartRoute() {
        // caret is at start of rout in the test java file
        myFixture.configureByText("DummyTestData.java", CODE);

        // this does not work with <caret> in the source code
        // PsiElement element = myFixture.getElementAtCaret();
        PsiElement element = myFixture.findElementByText("\"log:out\"", PsiLiteralExpression.class);

        assertFalse(CamelIdeaUtils.getService().isCamelRouteStart(element));
    }

}
