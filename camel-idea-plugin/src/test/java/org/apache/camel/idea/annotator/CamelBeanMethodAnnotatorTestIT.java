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
package org.apache.camel.idea.annotator;

import java.util.List;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;


/**
 * Test if the {@link CamelBeanMethodAnnotator} work as expected with private, overload and none exiting bean reference calls
 * <pre>
 *     To run this test from IDEA add the vm Options to run configuration
 *     -Didea.home.path=/Users/fharms/work/idea/
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
        myFixture.configureByFiles("AnnotatorJavaBeanRoute1TestData.java","AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, false);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(2,list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }

    /**
     * Test if the annotator mark the bean call "thisIsVeryPrivate","methodDoesNotExist" as errors
     */
    public void testAnnotatorJavaBeanWithAbstractMethod() {
        myFixture.configureByFiles("AnnotatorJavaBeanRoute2TestData.java","AnnotatorJavaBeanTestData.java", "AnnotatorJavaBeanSuperClassTestData.java");
        myFixture.checkHighlighting(false, false, true, false);

        List<HighlightInfo> list = myFixture.doHighlighting();
        assertEquals(2,list.stream().filter(i -> i.getSeverity().getName().equals("ERROR")).count());
    }


}