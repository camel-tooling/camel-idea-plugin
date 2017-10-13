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
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import static org.apache.camel.idea.gutter.GutterTestUtil.getGuttersWithJavaTarget;
import static org.apache.camel.idea.gutter.GutterTestUtil.getGuttersWithXMLTarget;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in XML DSL and the route navigation
 */
public class MultiLanguageCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testCamelGutterForJavaAndXMLRoutes() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml", "JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> javaGutters = myFixture.findAllGutters("JavaCamelRouteLineMarkerProviderTestData.java");
        assertNotNull(javaGutters);

        List<GutterMark> xmlGutters = myFixture.findAllGutters("XmlCamelRouteLineMarkerProviderTestData.xml");
        assertNotNull(xmlGutters);

        //remove first element since it is navigate to super implementation gutter icon
        javaGutters.remove(0);

        assertEquals("Should contain 2 Java Camel gutters", 2, javaGutters.size());
        assertEquals("Should contain 2 XML Camel gutters", 2, xmlGutters.size());

        //from Java to XML
        LineMarkerInfo.LineMarkerGutterIconRenderer firstJavaGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) javaGutters.get(0);
        assertTrue(firstJavaGutter.getLineMarkerInfo().getElement() instanceof PsiLiteralExpression);
        assertEquals("The navigation start element doesn't match", "file:inbox",
                ((PsiLiteralExpression) firstJavaGutter.getLineMarkerInfo().getElement()).getValue());


        List<GotoRelatedItem> firstJavaGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstJavaGutter);
        assertEquals("Navigation should have two targets", 2, firstJavaGutterTargets.size());
        assertEquals("The navigation target XML tag name doesn't match", "to", getGuttersWithXMLTarget(firstJavaGutterTargets).get(0).getLocalName());
        assertEquals("The navigation Java target element doesn't match", "from(\"file:outbox\")",
                getGuttersWithJavaTarget(firstJavaGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());

        //from XML to Java
        LineMarkerInfo.LineMarkerGutterIconRenderer firstXmlGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) xmlGutters.get(0);
        assertTrue(firstXmlGutter.getLineMarkerInfo().getElement() instanceof XmlToken);
        assertEquals("The navigation start element doesn't match", "file:inbox",
                ((PsiLiteralExpression) firstJavaGutter.getLineMarkerInfo().getElement()).getValue());


        List<GotoRelatedItem> firstXmlGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstXmlGutter);
        assertEquals("Navigation should have two targets", 2, firstXmlGutterTargets.size());
        assertEquals("The navigation target XML tag name doesn't match", "to", getGuttersWithXMLTarget(firstXmlGutterTargets).get(0).getLocalName());
        assertEquals("The navigation Java target element doesn't match", "from(\"file:outbox\")",
                getGuttersWithJavaTarget(firstXmlGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

}