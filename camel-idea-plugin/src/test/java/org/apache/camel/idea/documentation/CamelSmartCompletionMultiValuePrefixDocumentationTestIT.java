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
package org.apache.camel.idea.documentation;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

/**
 * Test the documentation from the smart completion list
 */
public class CamelSmartCompletionMultiValuePrefixDocumentationTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public String getJavaTestData() {
        return "public static class MyRouteBuilder extends RouteBuilder {\n"
            + "        @Override\n"
            + "        public void configure() throws Exception {\n"
            + "            from(\"file:inbox?<caret>\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";

    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/documentation/";
    }

    public void testSmartCompletionDocumentation() throws Exception {
        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestData());

        PsiElement element = myFixture.findElementByText("\"file:inbox?\"", PsiLiteralExpression.class);
        String componentName = "file";
        String lookup = componentName + ":inbox?scheduler.";
        PsiManager manager = myFixture.getPsiManager();

        PsiElement docInfo = new CamelDocumentationProvider().getDocumentationElementForLookupItem(manager, lookup, element);

        CamelDocumentationProvider.DocumentationElement de = (CamelDocumentationProvider.DocumentationElement) docInfo;
        assertEquals(componentName, de.getComponentName());
        assertEquals("schedulerProperties", de.getText());
        assertEquals("schedulerProperties", de.getEndpointOption());
    }

}