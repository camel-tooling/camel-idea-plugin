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
import com.intellij.openapi.components.ServiceManager;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.service.CamelPreferenceService;

/**
 * Testing the Camel icon is shown in the gutter where a Camel route starts in XML DSL
 */
public class XmlCamelRouteLineMarkerProviderTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testCamelGutter() {
        myFixture.configureByFiles("XmlCamelRouteLineMarkerProviderTestData.xml");
        List<GutterMark> gutters = myFixture.findGuttersAtCaret();
        assertNotNull(gutters);

        assertEquals(1, gutters.size());
        GutterMark gutter = gutters.get(0);

        assertEquals("<html>Camel route</html>", gutter.getTooltipText());

        Icon defaultIcon = ServiceManager.getService(CamelPreferenceService.class).getCamelIcon();
        Icon icon = gutter.getIcon();

        assertSame(defaultIcon, icon);
    }

}