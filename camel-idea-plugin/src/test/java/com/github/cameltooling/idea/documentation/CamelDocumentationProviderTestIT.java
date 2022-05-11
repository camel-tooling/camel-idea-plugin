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
package com.github.cameltooling.idea.documentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;

/**
 * Test if the expected documentation is called where the caret is placed
 */
public class CamelDocumentationProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private String getJavaTestWithCursorBeforeCamelComponent() {
        return "public static class MyRouteBuilder extends RouteBuilder {\n"
                + "        @Override\n"
                + "        public void configure() throws Exception {\n"
                + "            <caret>from(\"file:inbox?delete=true\")\n"
                + "                .to(\"file:outbox\");\n"
                + "        }\n"
                + "    }";
    }

    private String getJavaTestDataWithCursorAfterQuestionMark() {
        return "public static class MyRouteBuilder extends RouteBuilder {\n"
                + "        @Override\n"
                + "        public void configure() throws Exception {\n"
                + "            from(\"file:inbox?<caret>\")\n"
                + "                .to(\"file:outbox\");\n"
                + "        }\n"
                + "    }";
    }

    private String getJavaTestDataWithCursorBeforeColon() {
        return "public static class MyRouteBuilder extends RouteBuilder {\n"
                + "        @Override\n"
                + "        public void configure() throws Exception {\n"
                + "            from(\"file<caret>:inbox?\")\n"
                + "                .to(\"file:outbox\");\n"
                + "        }\n"
                + "    }";
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/documentation/";
    }

//    @Ignore
//    public void testJavaClassQuickNavigateInfo() throws Exception {
//        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestWithCursorBeforeCamelComponent());
//
//        PsiClass psiClass = getTestClass();
//        PsiReference referenceElement = myFixture.getReferenceAtCaretPosition();
//        assertNotNull(referenceElement);
//
//        String docInfo = new CamelDocumentationProvider().getQuickNavigateInfo(psiClass, referenceElement.getElement());
//        assertNotNull(docInfo);
//        assertEquals(exampleHtmlFileText(getTestName(true)), docInfo);
//    }

//    @Ignore
//    public void testGetUrlFor() {
//        assertNull(new CamelDocumentationProvider().getUrlFor(null, null));
//    }

//    @Ignore
//    public void testGenerateDoc() throws Exception {
//        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestDataWithCursorAfterQuestionMark());
//
//        PsiElement element = myFixture.findElementByText("\"file:inbox?\"", PsiLiteralExpression.class);
//        String componentName = "file";
//        String lookup = componentName + ":inbox?delete";
//        PsiManager manager = myFixture.getPsiManager();
//
//        PsiElement docInfo = new CamelDocumentationProvider().getDocumentationElementForLookupItem(manager, lookup, element);
//
//        String doc = new CamelDocumentationProvider().generateDoc(docInfo, null);
//        assertEquals(readExpectedFile(), doc);
//
//        String documentation = new CamelDocumentationProvider().generateDoc(element, null);
//        assertNotNull(documentation);
//        assertTrue(documentation.startsWith("<b>File Component</b><br/>The file component is used for reading or writing files.<br/>"));
//    }

    public void testHandleExternalLink() {
        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestDataWithCursorBeforeColon());

        PsiElement element = myFixture.findElementByText("\"file:inbox?\"", PsiLiteralExpression.class);

        CamelDocumentationProvider provider = new CamelDocumentationProvider();
        boolean externalLink = provider.handleExternalLink(null, "http://bennet-schulz.com", element);
        assertFalse(externalLink);
    }

    public void testCanFetchDocumentationLink() {
        CamelDocumentationProvider provider = new CamelDocumentationProvider();
        boolean documentationLink = provider.canFetchDocumentationLink("http://bennet-schulz.com");
        assertFalse(documentationLink);
    }

    private PsiClass getTestClass() throws Exception {
        return ((PsiJavaFile) myFixture.getFile()).getClasses()[0];
    }

    private String exampleHtmlFileText(String name) throws IOException {
        final File htmlPath = new File(getTestDataPath() + name + ".html");
        final String hmltDoc = StringUtil.convertLineSeparators(FileUtil.loadFile(htmlPath).trim(), "");
        return String.format(hmltDoc, CAMEL_VERSION);
    }

    private String readExpectedFile() {
        String fileContent = null;
        try {
            fileContent = getFileContent(new FileInputStream("src/test/resources/testData/expected_doc.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }

    private static String getFileContent(FileInputStream fis) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
