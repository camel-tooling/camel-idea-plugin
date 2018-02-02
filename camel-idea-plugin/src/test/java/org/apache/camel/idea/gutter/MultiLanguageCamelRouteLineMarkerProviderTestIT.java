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
package org.apache.camel.idea.gutter;

import java.util.List;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;


import static org.apache.camel.idea.gutter.GutterTestUtil.getGuttersWithJavaTarget;
import static org.apache.camel.idea.gutter.GutterTestUtil.getGuttersWithXMLTarget;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in XML DSL and the route navigation
 */
public class MultiLanguageCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void test_should_contain_java_gutters() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");
        assertNotNull(javaGutters);
    }

    public void test_should_contain_xml_gutters() {
        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        assertNotNull(xmlGutters);
    }

    public void test_should_contain_two_java_gutters() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");

        assertEquals("Should contain 3 gutters", 3, javaGutters.size());
        //remove first element since it is navigate to super implementation gutter icon
        javaGutters.remove(0);
        assertEquals("Should contain 2 Java Camel gutters", 2, javaGutters.size());
    }

    public void test_should_contain_two_xml_gutters(){
        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        assertEquals("Should contain 2 XML Camel gutters", 2, xmlGutters.size());
    }

    public void test_first_java_gutter_element_should_be_java_element() {
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstJavaGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) javaGutters.get(0);
        assertTrue(firstJavaGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
    }

    public void test_first_java_gutter_element_should_contain_file_inbox_as_navigation_start() {
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstJavaGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) javaGutters.get(1);
        assertEquals("The navigation start element doesn't match", "\"file:inbox\"",
                firstJavaGutter.getLineMarkerInfo().getElement().getText());
    }

    public void test_navigation_should_have_two_targets() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstJavaGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) javaGutters.get(1);
        List<GotoRelatedItem> firstJavaGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstJavaGutter);
        assertEquals("Navigation should have two targets", 2, firstJavaGutterTargets.size());
    }

    public void test_navigation_target_xml_should_match_to(){
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");
        javaGutters.remove(0);
        LineMarkerInfo.LineMarkerGutterIconRenderer firstJavaGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) javaGutters.get(0);
        List<GotoRelatedItem> firstJavaGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstJavaGutter);
        assertEquals("The navigation target XML tag name doesn't match", "to", getGuttersWithXMLTarget(firstJavaGutterTargets).get(0).getLocalName());
    }

    public void test_first_xml_gutter_should_be_xml_element() {
        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstXmlGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) xmlGutters.get(0);
        assertTrue(firstXmlGutter.getLineMarkerInfo().getElement() instanceof XmlToken);
    }

    public void test_xml_navigation_should_have_two_targets() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstXmlGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) xmlGutters.get(0);
        List<GotoRelatedItem> firstXmlGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstXmlGutter);
        assertEquals("Navigation should have two targets", 2, firstXmlGutterTargets.size());
    }

    public void test_xml_navigation_target_should_match_to() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstXmlGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) xmlGutters.get(0);
        List<GotoRelatedItem> firstXmlGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstXmlGutter);
        assertEquals("The navigation target XML tag name doesn't match", "to", getGuttersWithXMLTarget(firstXmlGutterTargets).get(0).getLocalName());
    }

    public void test_java_navigation_target_should_match_to() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        LineMarkerInfo.LineMarkerGutterIconRenderer firstXmlGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) xmlGutters.get(0);
        List<GotoRelatedItem> firstXmlGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstXmlGutter);
        assertEquals("The navigation Java target element doesn't match", "from(\"file:outbox\")",
                getGuttersWithJavaTarget(firstXmlGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }
}