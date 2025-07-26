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

import javax.swing.Icon;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.gutter.GutterTestUtil.getCamelGutters;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in Java DSL and the route navigation
 */
public class JavaCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MODEL_MAVEN_ARTIFACT};
    }

    public void testCamelGutter() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderTestData.java");
        List<GutterMark> gutters = getCamelGutters(myFixture);
        assertNotNull(gutters);

        assertEquals("Should contain 4 Camel gutters", 4, gutters.size());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstRestGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(0);

        assertTrue(firstRestGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"/say\"",
            firstRestGutter.getLineMarkerInfo().getElement().getText());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> secondRestGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(3);

        assertTrue(secondRestGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "rest",
            secondRestGutter.getLineMarkerInfo().getElement().getText());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(1);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:inbox\"",
            firstGutter.getLineMarkerInfo().getElement().getText());


        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:outbox\")",
            GutterTestUtil.getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(2);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:outbox\"",
            secondGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> secondGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:inbox\")",
            GutterTestUtil.getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

    public void testCamelGutterForToDAndToF() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderAlternateToTestData.java");
        List<GutterMark> gutters = getCamelGutters(myFixture);
        assertNotNull(gutters);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(0);

        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:test\"",
            firstGutter.getLineMarkerInfo().getElement().getText());


        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:test\")",
            GutterTestUtil.getGuttersWithJavaTarget(firstGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());


        LineMarkerInfo.LineMarkerGutterIconRenderer<?> secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(1);

        assertTrue(secondGutter.getLineMarkerInfo().getElement() instanceof PsiJavaToken);
        assertEquals("The navigation start element doesn't match", "\"file:test\"",
            secondGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> secondGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target element doesn't match", "from(\"file:test\")",
            GutterTestUtil.getGuttersWithJavaTarget(secondGutterTargets).get(0).getMethodExpression().getQualifierExpression().getText());
    }

    public void testCamelGutterForVariableAndConstant() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderFromVariableTestData.java");
        List<GutterMark> gutters = getCamelGutters(myFixture);
        assertNotNull(gutters);

        assertEquals("Should contain 2 Camel gutters", 2, gutters.size());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(0);
        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiIdentifier);
        assertEquals("The navigation start element doesn't match", "uriVar",
            firstGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have two targets", 2, firstGutterTargets.size());
    }

    public void testCamelGutterForMethodCallFrom() {
        myFixture.configureByFiles("JavaCamelRouteLineMarkerProviderFromMethodCallTestData.java");
        List<GutterMark> gutters = getCamelGutters(myFixture);
        assertNotNull(gutters);

        assertEquals("Should contain 1 Camel gutters", 1, gutters.size());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(0);
        assertTrue(firstGutter.getLineMarkerInfo().getElement() instanceof PsiIdentifier);
        assertEquals("The navigation start element doesn't match", "calcEndpoint",
            firstGutter.getLineMarkerInfo().getElement().getText());

        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have two targets", 2, firstGutterTargets.size());
        assertEquals("The navigation variable target element doesn't match", "calcEndpoint",
            GutterTestUtil.getGuttersWithMethodTarget(firstGutterTargets).get(0).getName());
    }

}
