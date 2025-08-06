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
import java.util.Optional;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.Nullable;


/**
 * Test if the {@link CamelBeanMethodAnnotator} work as expected with private, overload and none exiting bean reference calls
 * <pre>
 * if this test run with the "-Didea.home.path=/Users/home/work/idea/" it will report 2 as error count, because the idea.home
 * path point to the SDK and it able to resolve basic JDK classes.
 * </pre>
 */
public class CamelBeanMethodAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected @Nullable String[] getMavenDependencies() {
        return new String[] {CAMEL_CORE_MODEL_MAVEN_ARTIFACT};
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator/method";
    }

    /**
     * Test if the annotator mark the bean call "thisIsVeryPrivate","methodDoesNotExist" as errors
     */
    public void testAnnotatorJavaBeanWithPrivateAndNoneExistingMethod() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute1TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, false, true);
    }

    /**
     * Test if the annotator mark the bean call "thisIsVeryPrivate","methodDoesNotExist" as errors
     */
    public void testAnnotatorJavaBeanWithAbstractMethod() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute2TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, false, true);

        List<HighlightInfo> list = myFixture.doHighlighting();

        verifyHighlight(list, "\"letsDoThis\"", "Can not resolve method 'letsDoThis' in bean 'AnnotatorJavaBeanSuperClassTestData'", HighlightSeverity.ERROR);
        verifyHighlight(list, "\"mySuperAbstractMethod\"", "Can not resolve method 'mySuperAbstractMethod' in bean 'AnnotatorJavaBeanSuperClassTestData'", HighlightSeverity.ERROR);
        verifyHighlight(list, "\"thisIsVeryPrivate\"", "Can not resolve method 'thisIsVeryPrivate' in bean 'AnnotatorJavaBeanSuperClassTestData'", HighlightSeverity.ERROR);
    }

    /**
     * Test if the annotator mark the bean call "myOverLoadedBean" as errors because it's Ambiguous.
     * This test also test if the scenario where one of the overloaded methods is private and the other is public - the public
     * method should be chosen and there should be no ambiguity.
     */
    public void testAnnotatorJavaBeanAmbiguousMatch() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute3TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, false, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        verifyHighlight(list, "\"myOverLoadedBean\"", "Ambiguous matches 'myOverLoadedBean' in bean 'AnnotatorJavaBeanTestData'", HighlightSeverity.ERROR);
    }

    /**
     * Test if the annotator is false and don't mark any methods even it's ambiguous, but one of the methods are marked as @Handle
     */
    public void testAnnotatorJavaBeanWithHandlerAnnotation() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute4TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, false, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        list.stream().noneMatch(info -> info.getText().contains("myOverLoadedBean3"));
    }

    /**
     * Test if the calling methods is ambiguous and the Camel DSL bean calling method is with parameters
     */
    public void testAnnotatorJavaBeanAmbiguousMatchWithParameter() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute5TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, false, true);
    }

    public void testAnnotatorJavaBeanWithTypeParameters() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute6TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, false, true);
    }

    private void verifyHighlight(List<HighlightInfo> list, String expectedText, String expectedDescription, HighlightSeverity actualServerity) {
        final Optional<HighlightInfo> error1 = list.stream().filter(highlightInfo -> highlightInfo.getText().equals(expectedText)).findFirst();
        assertTrue(String.format("Expect to find the %s in the list of highlight", expectedText), error1.isPresent());
        assertEquals(expectedDescription, error1.get().getDescription());
        assertEquals(error1.get().getSeverity(), actualServerity);
    }


}
