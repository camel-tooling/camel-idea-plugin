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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;

import javax.swing.*;


/**
 * Utility class to test the gutter navigation.
 */
@SuppressWarnings("unchecked")
final class GutterTestUtil {

    private GutterTestUtil() {
        //empty
    }

    static List<GutterMark> getCamelGutters(JavaCodeInsightTestFixture fixture) {
        return filterCamelGutters(fixture.findAllGutters());
    }

    static List<GutterMark> getCamelGutters(JavaCodeInsightTestFixture fixture, String file) {
        return filterCamelGutters(fixture.findAllGutters(file));
    }

    private static List<GutterMark> filterCamelGutters(List<GutterMark> gutters) {
        Icon icon = CamelPreferenceService.getService().getCamelIcon();
        return gutters.stream()
                .filter(g -> g.getIcon().equals(icon)).toList();
    }

    static List<GotoRelatedItem> getGutterNavigationDestinationElements(LineMarkerInfo.LineMarkerGutterIconRenderer<?> gutter) {
        RelatedItemLineMarkerInfo<?> info = (RelatedItemLineMarkerInfo<?>) gutter.getLineMarkerInfo();
        Collection<? extends GotoRelatedItem> items = info.createGotoRelatedItems();
        return items.stream()
                .map(i -> (GotoRelatedItem) i)
                .toList();
    }

    /**
     * For the given gutters return all the gutter navigation targets that are {@link XmlTag} elements.
     *
     * @param gutterList
     * @return
     */
    static List<XmlTag> getGuttersWithXMLTarget(List<GotoRelatedItem> gutterList) {
        return gutterList
            .stream()
            .map(gotoRelatedItem -> PsiTreeUtil.getParentOfType(gotoRelatedItem.getElement(), XmlTag.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * For the given gutters return all the gutter navigation targets that are {@link PsiMethodCallExpressionImpl} elements.
     */
    static List<PsiMethodCallExpression> getGuttersWithJavaTarget(List<GotoRelatedItem> gutterList) {
        return gutterList
            .stream()
            .map(gotoRelatedItem -> PsiTreeUtil.getParentOfType(gotoRelatedItem.getElement(), PsiMethodCallExpression.class))
            .collect(Collectors.toList());
    }

    static List<PsiMethod> getGuttersWithMethodTarget(List<GotoRelatedItem> gutterList) {
        return getGuttersWithPsiElementTarget(gutterList, PsiMethod.class);
    }

    private static <E extends PsiElement> List<E> getGuttersWithPsiElementTarget(List<GotoRelatedItem> gutterList,
                                                                                 Class<E> elementType) {
        return gutterList
            .stream()
            .filter(gotoRelatedItem -> elementType.isInstance(gotoRelatedItem.getElement()))
            .map(GotoRelatedItem::getElement)
            .map(elementType::cast)
            .collect(Collectors.toList());
    }
}
