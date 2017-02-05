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

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.LineMarkersPass;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.navigation.GotoRelatedItem;


/**
 * Utility class to test the gutter navigation.
 */
final class GutterTestUtil {

    private GutterTestUtil() {
        //empty
    }

    static List<GotoRelatedItem> getGutterNavigationDestinationElements(LineMarkerInfo.LineMarkerGutterIconRenderer gutter) {
        LineMarkerProvider lineMarkerProvider1 = LineMarkersPass.getMarkerProviders(JavaLanguage.INSTANCE, gutter
                .getLineMarkerInfo()
                .getElement().getProject())
                .stream()
                .filter(lineMarkerProvider -> lineMarkerProvider instanceof CamelRouteLineMarkerProvider).findAny()
                .get();
        List<RelatedItemLineMarkerInfo> result = new ArrayList<>();
        ((CamelRouteLineMarkerProvider) lineMarkerProvider1).collectNavigationMarkers(gutter.getLineMarkerInfo().getElement(), result);
        return (List<GotoRelatedItem>) result.get(0).createGotoRelatedItems();
    }
}
