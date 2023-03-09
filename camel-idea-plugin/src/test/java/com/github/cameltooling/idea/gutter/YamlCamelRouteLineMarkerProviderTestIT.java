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
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in YAML DSL and the route navigation
 */
public class YamlCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testCamelGutter() {
        myFixture.configureByFiles("YamlCamelRouteLineMarkerProviderTestData.yaml");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        assertEquals("Does not contain the expected amount of Camel gutters", 4, gutters.size());

        Icon defaultIcon = CamelPreferenceService.getService().getCamelIcon();
        gutters.forEach(gutterMark -> {
            assertSame("Gutter should have the Camel icon", defaultIcon, gutterMark.getIcon());
            assertEquals("Camel route", gutterMark.getTooltipText());
        });

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> firstGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(1);

        assertSame(YAMLTokenTypes.SCALAR_KEY, firstGutter.getLineMarkerInfo().getElement().getNode().getElementType());
        assertEquals("from", firstGutter.getLineMarkerInfo().getElement().getText());
        assertEquals("The navigation start element doesn't match", "file:inbox",
                PsiTreeUtil.getChildOfType(firstGutter.getLineMarkerInfo().getElement().getParent(), YAMLMapping.class).getKeyValueByKey("uri").getValueText());

        List<GotoRelatedItem> firstGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(firstGutter);
        assertEquals("Navigation should have one target", 1, firstGutterTargets.size());
        assertEquals("The navigation target route doesn't match", "to: file:inbox", firstGutterTargets.get(0).getElement().getText());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> secondGutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(2);

        assertSame(YAMLTokenTypes.SCALAR_KEY, secondGutter.getLineMarkerInfo().getElement().getNode().getElementType());
        assertEquals("from", secondGutter.getLineMarkerInfo().getElement().getText());
        assertEquals("The navigation start element doesn't match", "file:outbox",
            PsiTreeUtil.getChildOfType(secondGutter.getLineMarkerInfo().getElement().getParent(), YAMLMapping.class).getKeyValueByKey("uri").getValueText());

        List<GotoRelatedItem> secondGutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(secondGutter);
        assertEquals("Navigation should have one target", 1, secondGutterTargets.size());
        assertEquals("The navigation target route doesn't match", "to:\n" +
            "            uri: \"file:outbox\"", secondGutterTargets.get(0).getElement().getText());
    }

    public void testCamelGutterForToD() {
        myFixture.configureByFiles("YamlCamelRouteLineMarkerProviderToDTestData.yaml");
        List<GutterMark> gutters = myFixture.findAllGutters();
        assertNotNull(gutters);

        assertEquals("Should contain 1 Camel gutter", 1, gutters.size());

        assertSame("Gutter should have the Camel icon", CamelPreferenceService.getService().getCamelIcon(), gutters.get(0).getIcon());
        assertEquals("Camel route", gutters.get(0).getTooltipText());

        LineMarkerInfo.LineMarkerGutterIconRenderer<?> gutter = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>) gutters.get(0);

        assertSame(YAMLTokenTypes.SCALAR_KEY, gutter.getLineMarkerInfo().getElement().getNode().getElementType());
        assertEquals("from", gutter.getLineMarkerInfo().getElement().getText());
        assertEquals("The navigation start element doesn't match", "file:inbox",
            PsiTreeUtil.getChildOfType(gutter.getLineMarkerInfo().getElement().getParent(), YAMLMapping.class)
                .getKeyValueByKey("uri")
                .getValueText()
        );

        List<GotoRelatedItem> gutterTargets = GutterTestUtil.getGutterNavigationDestinationElements(gutter);
        assertEquals("Navigation should have one target", 1, gutterTargets.size());
        assertEquals("The navigation target route doesn't match",  "tod:\n" +
            "            uri: \"file:inbox\"", gutterTargets.get(0).getElement().getText());

    }

}
