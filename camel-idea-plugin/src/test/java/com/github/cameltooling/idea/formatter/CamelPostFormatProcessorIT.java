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
package com.github.cameltooling.idea.formatter;

import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The integration test for the text formatter.
 */
public class CamelPostFormatProcessorIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String CAMEL_SUPPORT_MAVEN_ARTIFACT = String.format("org.apache.camel:camel-support:%s", CAMEL_VERSION);
    private static final String CAMEL_API_MAVEN_ARTIFACT = String.format("org.apache.camel:camel-api:%s", CAMEL_VERSION);

    /**
     * Ensures that expressions are not formatted and the language DSL is supported.
     */
    public void testRouteWithExpressions() {
        doTest("RouteWithExpressions", null);
    }

    /**
     * Ensures that the data format DSL is supported.
     */
    public void testRouteWithDataFormatDSL() {
        doTest("RouteWithDataFormatDSL", null);
    }

    /**
     * Ensures that a malformed route won't make the formatter fail.
     */
    public void testMalformedRoute() {
        doTest("MalformedRoute", null);
    }

    /**
     * Ensures that an entire file can be formatted using the default settings.
     */
    public void testFormatEntireFile() {
        doTest("EntireFile", null);
    }

    /**
     * Ensures that the test case defined in <a href="https://github.com/camel-tooling/camel-idea-plugin/issues/879">#879</a>
     * is properly fixed.
     */
    public void testFormatting879() {
        doTest("Formatting879", null);
    }

    /**
     * Ensures that a partial format not including a route has no effect.
     */
    public void testPartialFormat() {
        doTest("PartialFormat", new TextRange(0, 100));
    }

    /**
     * Ensures that a partial format including a route can format only the selected route.
     */
    public void testPartialFormatWithRoute() {
        doTest("PartialFormatWithRoute", new TextRange(1400, 1757));
    }

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_SUPPORT_MAVEN_ARTIFACT, CAMEL_API_MAVEN_ARTIFACT};
    }

    @Override
    protected @NotNull String getTestDataPath() {
        return "src/test/resources/testData/formatter/";
    }

    private void doTest(String name, TextRange range) {
        var before = "before/%s.java".formatted(name);
        var after = "after/%s.txt".formatted(name);
        myFixture.configureByFile(before);
        performReformatting(range);
        myFixture.checkResultByFile(after);
        //check idempotence of formatter
        performReformatting(range);
        myFixture.checkResultByFile(after);
    }

    private void performReformatting(TextRange range) {
        Project project = getProject();
        WriteCommandAction.runWriteCommandAction(
            project, () -> {
                TextRange textRange = range;
                if (textRange == null) {
                    textRange = getFile().getTextRange();
                }
                CodeStyleManager.getInstance(project).reformatText(getFile(), List.of(textRange));
            }
        );
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }
}
