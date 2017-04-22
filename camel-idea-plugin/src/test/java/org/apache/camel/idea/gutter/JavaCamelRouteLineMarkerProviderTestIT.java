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
import javax.swing.*;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.service.CamelPreferenceService;

import static org.apache.camel.idea.gutter.GutterTestUtil.getGutterNavigationDestinationElements;
import static org.apache.camel.idea.gutter.GutterTestUtil.getGuttersWithJavaTarget;
import static org.apache.camel.idea.gutter.GutterTestUtil.getGuttersWithJavaVariableTarget;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in Java DSL and the route navigation
 */
public class JavaCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testCamelGutter() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        Icon defaultIcon = ServiceManager.getService(CamelPreferenceService.class).getCamelIcon();
        gutters.forEach(gutterMark -> {
            assertSame("Gutter should have the Camel icon", defaultIcon, gutterMark.getIcon());
            assertEquals("Camel route", gutterMark.getTooltipText());
        });

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(0);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiLiteralExpression);
        assertEquals("The navigation start element doesn't match", "file:inbox",
            ((PsiLiteralExpression) firstGutter.getLineMarkerInfo().getElement()).getValue());


        List<GotoRelatedItem> firstGutterTargets = getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:outbox\")",
            getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());


        LineMarkerInfo.LineMarkerGutterIconRenderer secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(1);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiLiteralExpression);
        assertEquals("The navigation start element doesn't match", "file:outbox",
            ((PsiLiteralExpression) secondGutter.getLineMarkerInfo().getElement()).getValue());

        List<GotoRelatedItem> secondGutterTargets = getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:inbox\")",
            getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

    public void testCamelGutterForToDAndToF() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderAlternateToTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        Icon defaultIcon = ServiceManager.getService(CamelPreferenceService.class).getCamelIcon();
        gutters.forEach(gutterMark -> {
            assertSame("Gutter should have the Camel icon", defaultIcon, gutterMark.getIcon());
            assertEquals("Camel route", gutterMark.getTooltipText());
        });

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(0);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiLiteralExpression);
        assertEquals("The navigation start element doesn't match", "file:test",
            ((PsiLiteralExpression) firstGutter.getLineMarkerInfo().getElement()).getValue());


        List<GotoRelatedItem> firstGutterTargets = getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:test\")",
            getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());


        LineMarkerInfo.LineMarkerGutterIconRenderer secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(1);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiLiteralExpression);
        assertEquals("The navigation start element doesn't match", "file:test",
            ((PsiLiteralExpression) secondGutter.getLineMarkerInfo().getElement()).getValue());

        List<GotoRelatedItem> secondGutterTargets = getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:test\")",
            getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

    public void testCamelGutterForVariableAndConstant() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderFromVariableTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        Icon defaultIcon = ServiceManager.getService(CamelPreferenceService.class).getCamelIcon();
        gutters.forEach(gutterMark -> {
            assertSame("Gutter should have the Camel icon", defaultIcon, gutterMark.getIcon());
            assertEquals("Camel route", gutterMark.getTooltipText());
        });

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(0);
        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiIdentifier);
        assertEquals("The navigation start element doesn't match", "uriVar",
            firstGutter.getLineMarkerInfo().getElement().getText());


        List<GotoRelatedItem> firstGutterTargets = getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have two targets", 2, firstGutterTargets.size());
        assertEquals("The navigation for literal expression target element doesn't match", "from(CONSTANT_URI)",
            getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
        assertEquals("The navigation variable target element doesn't match", "uriVar",
            getGuttersWithJavaVariableTarget(firstGutterTargets).get(0).getName());


        LineMarkerInfo.LineMarkerGutterIconRenderer secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(1);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiIdentifier);
        assertEquals("The navigation start element doesn't match", "CONSTANT_URI",
            (secondGutter.getLineMarkerInfo().getElement()).getText());

        List<GotoRelatedItem> secondGutterTargets = getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 2, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(uriVar)",
            getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
        assertEquals("The navigation constant target element doesn't match", "CONSTANT_URI",
            getGuttersWithJavaVariableTarget(secondGutterTargets).get(0).getName());

    }

}