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

import java.io.File;
import java.io.IOException;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiReference;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

/**
 * Test if the expected documentation is called where the caret is placed
 */
public class CamelDocumentationProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {


    public String getJavaTestData() {
        return "public static class MyRouteBuilder extends RouteBuilder {\n"
            + "        @Override\n"
            + "        public void configure() throws Exception {\n"
            + "            <caret>from(\"file:inbox?delete=true\")\n"
            + "                .to(\"file:outbox\");\n"
            + "        }\n"
            + "    }";

    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/documentation/";
    }

    public void testJavaClassQuickNavigateInfo() throws Exception {
        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestData());

        PsiClass psiClass = getTestClass();
        PsiReference referenceElement = myFixture.getReferenceAtCaretPosition();
        assertNotNull(referenceElement);

        String docInfo = new CamelDocumentationProvider().getQuickNavigateInfo(psiClass, referenceElement.getElement());
        assertNotNull(docInfo);
        assertEquals(exampleHtmlFileText(getTestName(true)), docInfo);
    }

    private PsiClass getTestClass() throws Exception {
        return ((PsiJavaFile) myFixture.getFile()).getClasses()[0];
    }

    private String exampleHtmlFileText(String name) throws IOException {
        final File htmlPath = new File(getTestDataPath() + name + ".html");
        return StringUtil.convertLineSeparators(FileUtil.loadFile(htmlPath).trim(), "");
    }

}