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
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import org.junit.Ignore;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in Java DSL and the route navigation
 */
public class JavaCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Ignore
    public void testCamelGutter() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);

        assertEquals("Should contain 3 Camel gutters", 3, gutters.size());

        assertGuttersHasCamelIcon(gutters);

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(1);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:inbox\"",
            firstGutter.getLineMarkerInfo().getElement().getText());


        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:outbox\")",
            GutterTestUtil.getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());


        LineMarkerInfo.LineMarkerGutterIconRenderer secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(2);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:outbox\"",
            secondGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> secondGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:inbox\")",
            GutterTestUtil.getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

    @Ignore
    public void testCamelGutterForToDAndToF() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderAlternateToTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        assertGuttersHasCamelIcon(gutters);

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(0);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:test\"",
            firstGutter.getLineMarkerInfo().getElement().getText());


        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:test\")",
            GutterTestUtil.getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());


        LineMarkerInfo.LineMarkerGutterIconRenderer secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(1);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:test\"",
            secondGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> secondGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:test\")",
            GutterTestUtil.getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

    @Ignore
    public void testCamelGutterForVariableAndConstant() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderFromVariableTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        assertGuttersHasCamelIcon(gutters);

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(0);
        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiIdentifier);
        assertEquals("The navigation start element doesn't match", "uriVar",
            firstGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have two targets", 2, firstGutterTargets.size());
    }

    @Ignore
    public void testCamelGutterForMethodCallFrom() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderFromMethodCallTestData.java");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        //remove first element since it is navigate to super implementation gutter icon
        gutters.remove(0);
        // remove last element since it is from method returning route uri
        gutters.remove(gutters.size() - 1);

        assertEquals("Should contain 1 Camel gutters", 1, gutters.size());

        assertGuttersHasCamelIcon(gutters);

        LineMarkerInfo.LineMarkerGutterIconRenderer firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer) gutters.get(0);
        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiIdentifier);
        assertEquals("The navigation start element doesn't match", "calcEndpoint",
            firstGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have two targets", 2, firstGutterTargets.size());
        assertEquals("The navigation variable target element doesn't match", "calcEndpoint",
            GutterTestUtil.getGuttersWithMethodTarget(firstGutterTargets).get(0).getName());
    }

    private void assertGuttersHasCamelIcon(List<GutterMark> gutters) {
        Icon defaultIcon = ServiceManager.getService(CamelPreferenceService.class).getCamelIcon();
        gutters.forEach(gutterMark -> {
            assertSame("Gutter should have the Camel icon", defaultIcon, gutterMark.getIcon());
            assertEquals("Camel route", gutterMark.getTooltipText());
        });
    }

}
