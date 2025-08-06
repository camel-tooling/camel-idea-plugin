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
package com.github.cameltooling.idea.annotator;

import java.util.List;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BeanReferenceTypeAnnotatorIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected @Nullable String[] getMavenDependencies() {
        return new String[] {CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_API_MAVEN_ARTIFACT};
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator/beanreference";
    }

    public void testCorrectXmlBeanReference() {
        List<HighlightInfo> highlights = getHighlights("correct-reference.xml", "TestClass1.java", "TestClass2.java");
        assertEmpty(highlights);
    }

    public void testIncorrectXmlBeanReference() {
        List<HighlightInfo> highlights = getHighlights("incorrect-reference.xml", "TestClass1.java", "TestClass2.java");
        assertEquals(1, highlights.size());
        HighlightInfo highlight = highlights.get(0);
        assertEquals(HighlightSeverity.ERROR, highlight.getSeverity());
        assertEquals("testBean", highlight.getText());
        assertEquals("Bean must be of 'TestClass2' type", highlight.getDescription());
    }

    public void testAnnotations() {
        List<HighlightInfo> highlights = getHighlights("TestClass1.java", "TestClass2.java", "beans.xml");
        List<HighlightInfo> testClass1BeanErrors = highlights.stream()
                .filter(h -> h.getText().equals("testClass1Bean"))
                .collect(Collectors.toList());
        assertEquals(1, testClass1BeanErrors.size());
        assertEquals("Bean must be of 'TestClass2' type", testClass1BeanErrors.get(0).getDescription());

        assertEmpty(highlights.stream().filter(h -> h.getText().equals("testClass2Bean")).collect(Collectors.toList()));
        assertEmpty(highlights.stream().filter(h -> h.getText().equals("someEndpoint")).collect(Collectors.toList()));
    }

    @NotNull
    private List<HighlightInfo> getHighlights(String ... filePaths) {
        myFixture.configureByFiles(filePaths);
        myFixture.checkHighlighting(false, false, true, true);

        return myFixture.doHighlighting().stream()
                .filter(info -> !info.getText().equals("http://www.osgi.org/xmlns/blueprint/v1.0.0"))
                .collect(Collectors.toList());
    }

}