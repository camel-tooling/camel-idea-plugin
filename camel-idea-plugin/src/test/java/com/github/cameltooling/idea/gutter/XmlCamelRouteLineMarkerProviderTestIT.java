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
package com.github.cameltooling.idea.gutter;

import java.util.List;
import javax.swing.*;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.github.cameltooling.idea.service.CamelPreferenceService;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in XML DSL and the route navigation
 */
public class XmlCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testCamelGutter() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        assertEquals("Does not contain the expected amount of Camel gutters", 3, gutters.size());

        Icon defaultIcon = ApplicationManager.getApplication().getService(CamelPreferenceService.class).getCamelIcon();
        gutters.forEach(gutterMark -> {
            assertSame("Gutter should have the Camel icon", defaultIcon, gutterMark.getIcon());
            assertEquals("Camel route", gutterMark.getTooltipText());
        });

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(1);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof XmlToken);
        assertEquals("The navigation start element doesn't match", "file:inbox",
                PsiTreeUtil.getParentOfType(firstGutter.getLineMarkerInfo().getElement(), XmlTag.class).getAttribute("uri").getValue());

        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target route doesn't match", "file:inbox", firstGutterTargets.get(0).getElement().getText());
        assertEquals("The navigation target tag name doesn't match", "to",
                GutterTestUtil.getGuttersWithXMLTarget(firstGutterTargets).get(0).getLocalName());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(2);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof XmlToken);
        assertEquals("The navigation start element doesn't match", "file:outbox",
                PsiTreeUtil.getParentOfType(secondGutter.getLineMarkerInfo().getElement(), XmlTag.class).getAttribute("uri").getValue());

        List<GotoRelatedItem> secondGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target route doesn't match", "file:outbox", secondGutterTargets.get(0).getElement().getText());
        assertEquals("The navigation target tag name doesn't match", "to",
                GutterTestUtil.getGuttersWithXMLTarget(secondGutterTargets).get(0).getLocalName());
    }

    public void testCamelGutterForToD() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderToDTestData.xml");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        assertEquals("Should contain 1 Camel gutter", 1, gutters.size());

        assertSame("Gutter should have the Camel icon", ApplicationManager.getApplication().getService(CamelPreferenceService.class).getCamelIcon(), gutters.get(0).getIcon());
        assertEquals("Camel route", gutters.get(0).getTooltipText());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> gutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(0);

        assertTrue(gutter.getLineMarkerInfo().getElement() instanceof XmlToken);
        assertEquals("The navigation start element doesn't match", "file:inbox",
                PsiTreeUtil.getParentOfType(gutter.getLineMarkerInfo().getElement(), XmlTag.class).getAttribute("uri").getValue());

        List<GotoRelatedItem> gutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(gutter);
        assertEquals("Navigation should have one target", 1, gutterTargets.size());
        assertEquals("The navigation target route doesn't match", "file:inbox", gutterTargets.get(0).getElement().getText());
        assertEquals("The navigation target tag name doesn't match", "toD",
                GutterTestUtil.getGuttersWithXMLTarget(gutterTargets).get(0).getLocalName());

    }

}
