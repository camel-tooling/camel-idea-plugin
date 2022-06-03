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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.completion.SimpleSuggestion;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.hamcrest.Matchers;

import static org.junit.Assert.assertThat;

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

    private String getPropertiesTestDataWithCursorInPropertyName() {
        return "camel.main.all<caret>";
    }

    private String getPropertiesTestDataWithCursorInGroupName() {
        return "camel.m<caret>";
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/documentation/";
    }

    public void testJavaClassQuickNavigateInfo() throws Exception {
        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestWithCursorBeforeCamelComponent());

        PsiClass psiClass = getTestClass();
        PsiReference referenceElement = myFixture.getReferenceAtCaretPosition();
        assertNotNull(referenceElement);

        String docInfo = new CamelDocumentationProvider().getQuickNavigateInfo(psiClass, referenceElement.getElement());
        assertNotNull(docInfo);
        assertEquals(exampleHtmlFileText(getTestName(true)), docInfo);
    }

    public void testGenerateDoc() throws IOException {
        myFixture.configureByText(JavaFileType.INSTANCE, getJavaTestDataWithCursorAfterQuestionMark());

        PsiElement element = myFixture.findElementByText("\"file:inbox?\"", PsiLiteralExpression.class);
        String componentName = "file";
        String lookup = componentName + ":inbox?delete";
        PsiManager manager = myFixture.getPsiManager();

        PsiElement docInfo = new CamelDocumentationProvider().getDocumentationElementForLookupItem(manager, lookup, element);

        String doc = new CamelDocumentationProvider().generateDoc(docInfo, null);
        assertEquals(readExpectedFile(), doc);

        String documentation = new CamelDocumentationProvider().generateDoc(element, null);
        assertNotNull(documentation);
        assertTrue(documentation.startsWith("<b>File Component</b><br/>Read and write files.<br/>"));
    }

    public void testGenerateOptionDoc() {
        myFixture.configureByText("test-option.properties", getPropertiesTestDataWithCursorInPropertyName());

        final PsiElement originalElement = myFixture.getElementAtCaret();
        PsiElement element = DocumentationManager
            .getInstance(getProject())
            .findTargetElement(myFixture.getEditor(), originalElement.getContainingFile(), originalElement);

        if (element == null) {
            element = originalElement;
        }

        PsiManager manager = myFixture.getPsiManager();

        BaseOptionModel option = new BaseOptionModel() {

        };
        option.setName("Some Name");
        option.setGroup("Some Group");
        option.setJavaType("Some Type");
        option.setDescription("Some Description");
        OptionSuggestion lookup = new OptionSuggestion(option, "camel.main.allow-use-original-message = ");

        PsiElement docInfo = new CamelDocumentationProvider().getDocumentationElementForLookupItem(manager, lookup, element);

        String doc = new CamelDocumentationProvider().generateDoc(docInfo, null);
        assertNotNull(doc);
        assertThat(doc, Matchers.containsString("Some Name"));
        assertThat(doc, Matchers.containsString("Some Group"));
        assertThat(doc, Matchers.containsString("Some Type"));
        assertThat(doc, Matchers.containsString("Some Description"));
    }

    public void testGenerateSimpleDoc() {
        myFixture.configureByText("test-option.properties", getPropertiesTestDataWithCursorInGroupName());

        final PsiElement originalElement = myFixture.getElementAtCaret();
        PsiElement element = DocumentationManager
            .getInstance(getProject())
            .findTargetElement(myFixture.getEditor(), originalElement.getContainingFile(), originalElement);

        if (element == null) {
            element = originalElement;
        }

        PsiManager manager = myFixture.getPsiManager();

        SimpleSuggestion lookup = new SimpleSuggestion("Some Name", () -> "Some Description", "camel.main.");

        PsiElement docInfo = new CamelDocumentationProvider().getDocumentationElementForLookupItem(manager, lookup, element);

        String doc = new CamelDocumentationProvider().generateDoc(docInfo, null);
        assertNotNull(doc);
        assertThat(doc, Matchers.containsString("Some Name"));
        assertThat(doc, Matchers.containsString("Some Description"));
    }

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

    private PsiClass getTestClass() {
        return ((PsiJavaFile) myFixture.getFile()).getClasses()[0];
    }

    private String exampleHtmlFileText(String name) throws IOException {
        final File htmlPath = new File(getTestDataPath() + name + ".html");
        final String hmltDoc = StringUtil.convertLineSeparators(FileUtil.loadFile(htmlPath).trim(), "");
        return String.format(hmltDoc, CAMEL_VERSION);
    }

    private String readExpectedFile() throws IOException {
        return Files.readString(Paths.get("src/test/resources/testData/expected_doc.xml"));
    }
}
