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
package com.github.cameltooling.idea.gutter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.reference.blueprint.ReferenceableIdPsiElement;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;


public class BeanInjectLineMarkerProviderTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/gutter/beaninject";
    }

    public void testNoGutter() {
        myFixture.configureByFiles("TestClass2.java", "beans.xml");
        List<RelatedItemLineMarkerInfo<? extends PsiElement>> beanInjectMarkers = findBeanInjectMarkers();
        assertEmpty(beanInjectMarkers);
    }

    public void testBeanInjectGutter() {
        myFixture.configureByFiles("TestClass1.java", "TestClass2.java", "TestClass3.java", "beans.xml");

        List<RelatedItemLineMarkerInfo<? extends PsiElement>> beanInjectMarkers = findBeanInjectMarkers();
        assertEquals(4, beanInjectMarkers.size());

        validateBeanInjectWithValue(beanInjectMarkers.get(0));
        validateBeanInjectWithMultipleTargets(beanInjectMarkers.get(1));
        validateBeanInjectWithMultipleTargets(beanInjectMarkers.get(2));
        assertEquals(0, beanInjectMarkers.get(3).createGotoRelatedItems().size());
    }

    @NotNull
    private List<RelatedItemLineMarkerInfo<? extends PsiElement>> findBeanInjectMarkers() {
        List<GutterMark> gutters = myFixture.findAllGutters();
        return gutters.stream()
                .filter(g -> BeanInjectLineMarkerProvider.MARKER_TOOLTIP_TEXT.equals(g.getTooltipText()))
                .map(g -> (LineMarkerInfo.LineMarkerGutterIconRenderer<? extends PsiElement>) g)
                .map(LineMarkerInfo.LineMarkerGutterIconRenderer::getLineMarkerInfo)
                .map(info -> (RelatedItemLineMarkerInfo<? extends PsiElement>) info)
                .collect(Collectors.toList());
    }

    private void validateBeanInjectWithMultipleTargets(RelatedItemLineMarkerInfo<? extends PsiElement> marker) {
        List<String> targetBeanNames = getTargetBeanNames(marker);
        assertEquals(2, targetBeanNames.size());
        assertContainsElements(targetBeanNames, "testClass2Bean", "testClass2Bean2");
    }

    private void validateBeanInjectWithValue(RelatedItemLineMarkerInfo<? extends PsiElement> marker) {
        List<String> targetBeanNames = getTargetBeanNames(marker);
        assertEquals(1, targetBeanNames.size());
        assertEquals("testClass2Bean", targetBeanNames.get(0));
    }

    private List<String> getTargetBeanNames(RelatedItemLineMarkerInfo<? extends PsiElement> marker) {
        return marker.createGotoRelatedItems().stream()
                .map(GotoRelatedItem::getElement)
                .map(element -> element instanceof ReferenceableIdPsiElement ? element.getNavigationElement() : element)
                .filter(Objects::nonNull)
                .map(element -> StringUtil.unquoteString(element.getText()))
                .collect(Collectors.toList());

    }

}
