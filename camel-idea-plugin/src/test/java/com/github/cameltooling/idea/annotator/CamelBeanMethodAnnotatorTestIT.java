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
package com.github.cameltooling.idea.annotator;

import java.util.List;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;


/**
 * Test if the {@link CamelBeanMethodAnnotator} work as expected with private, overload and none exiting bean reference calls
 * <pre>
 * if this test run with the "-Didea.home.path=/Users/home/work/idea/" it will report 2 as error count, because the idea.home
 * path point to the SDK and it able to resolve basic JDK classes.
 * </pre>
 */
public class CamelBeanMethodAnnotatorTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/annotator/method";
    }

    /**
     * Test if the annotator mark the bean call "thisIsVeryPrivate","methodDoesNotExist" as errors
     */
    public void testAnnotatorJavaBeanWithPrivateAndNoneExistingMethod() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute1TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(3, list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }

    /**
     * Test if the annotator mark the bean call "thisIsVeryPrivate","methodDoesNotExist" as errors
     * TODO fharms, find out why it starting to fail after upgrading to 2019.1.3
     */
   /* public void testAnnotatorJavaBeanWithAbstractMethod() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute2TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(3, list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }*/

    /**
     * Test if the annotator mark the bean call "myOverLoadedBean" as errors because it's Ambiguous. This test also test if the scenario where one of the
     * overloaded methods is private and the other is public
     */
    public void testAnnotatorJavaBeanAmbiguousMatch() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute3TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(3, list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }

    /**
     * Test if the annotator is false and don't mark any methods even it's ambiguous, but one of the methods are marked as @Handle
     */
    public void testAnnotatorJavaBeanWithHandlerAnnotation() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute4TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(1, list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }

    /**
     * Test if the calling methods is ambiguous and the Camel DSL bean calling method is with parameters
     */
    public void testAnnotatorJavaBeanAmbiguousMatchWithParameter() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute5TestData.java", "AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, true);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(1, list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }


}
