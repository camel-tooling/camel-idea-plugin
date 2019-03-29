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
package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlueprintJavaClassReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Language("XML")
    private static final String REFERENCE =
        "<blueprint xmlns='http://www.osgi.org/xmlns/blueprint/v1.0.0'>" +
        "  <bean id='myBean' class='java.lang.S<caret>tring'/>" +
        "</blueprint>";

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/reference/blueprint";
    }

    public void testReference() {
        myFixture.configureByText("test.xml", REFERENCE);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        PsiReference reference = element.getReference();
        assertNotNull(reference);
        assertTrue(reference instanceof JavaClassReference);
    }

    public void testCompletion() {
        myFixture.configureByFiles("blueprint-java-class-completion-test.xml", "TestClass1.java", "TestClass2.java");
        List<String> strings = getCompletionResults();
        assertContainsElements(strings, "TestClass1", "TestClass2");
    }

    public void testCompletionWithNoResults() {
        myFixture.configureByFiles("blueprint-java-class-completion-test_no-results.xml", "TestClass1.java", "TestClass2.java");
        List<String> strings = getCompletionResults();
        assertEmpty(strings);
    }

    @NotNull
    private List<String> getCompletionResults() {
        myFixture.complete(CompletionType.CLASS_NAME);
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        return strings;
    }

}
