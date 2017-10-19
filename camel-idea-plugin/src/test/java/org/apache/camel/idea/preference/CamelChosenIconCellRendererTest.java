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
package org.apache.camel.idea.preference;

import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

public class CamelChosenIconCellRendererTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private CamelPreferenceEntryPage camelPreferenceEntryPage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        camelPreferenceEntryPage = new CamelPreferenceEntryPage();
        camelPreferenceEntryPage.createComponent();
        super.initCamelPreferencesService();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        camelPreferenceEntryPage = null;
        super.initCamelPreferencesService();
    }

//    public void testPluginXmlShouldContainPreferencesPage() {
//        File pluginXml = new File("src/main/resources/META-INF/plugin.xml");
//        assertNotNull(pluginXml);
//
//        try {
//            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/META-INF/plugin.xml"), StandardCharsets.UTF_8);
//            List<String> trimmedStrings =
//                    lines.stream().map(String::trim).collect(Collectors.toList());
//            int indexOfApplicationConfigurable = trimmedStrings.indexOf("<applicationConfigurable id=\"camel\" groupId=\"language\" displayName=\"Apache Camel\"");
//
//            assertTrue(indexOfApplicationConfigurable > 0);
//            assertEquals("<applicationConfigurable id=\"camel\" displayName=\"Apache Camel\" groupId=\"language\"", trimmedStrings.get(indexOfApplicationConfigurable));
//            String secondLineOfApplicationConfigurable = trimmedStrings.get(indexOfApplicationConfigurable + 1);
//            assertEquals("instance=\"org.apache.camel.idea.preference.CamelPreferenceEntryPage\"/>", secondLineOfApplicationConfigurable);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void testDisplayNameShouldBeApacheCamel() {
        assertEquals("Apache Camel", camelPreferenceEntryPage.getDisplayName());
    }

    public void testHelpTopicShouldBeNull() {
        assertNull(camelPreferenceEntryPage.getHelpTopic());
    }

    public void testPreferencePageIdShouldBeCamelConfigurable() {
        assertEquals("preference.CamelConfigurable", camelPreferenceEntryPage.getId());
    }
}
