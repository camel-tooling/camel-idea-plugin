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
package com.github.cameltooling.idea.preference.editorsettings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.ui.components.JBCheckBox;

public class CamelEditorSettingsPageTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private CamelEditorSettingsPage editorSettingsPage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        editorSettingsPage = new CamelEditorSettingsPage();
        editorSettingsPage.createComponent();
        super.initCamelPreferencesService();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        editorSettingsPage = null;
        super.initCamelPreferencesService();
    }

    public void testPluginXmlShouldContainEditorPreferencesPage() {
        File pluginXml = new File("src/main/resources/META-INF/plugin.xml");
        assertNotNull(pluginXml);

        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/META-INF/plugin.xml"), StandardCharsets.UTF_8);
            List<String> trimmedStrings =
                    lines.stream().map(String::trim).collect(Collectors.toList());
            int indexOfApplicationConfigurable = trimmedStrings.indexOf("<applicationConfigurable id=\"camel\" groupId=\"language\" displayName=\"Apache Camel\" instance=\"com.github.cameltooling.idea.preference.CamelPreferenceEntryPage\"/>");

            assertTrue(indexOfApplicationConfigurable > 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testShouldContainDownloadCatalogCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getDownloadCatalogCheckBox();
        assertEquals("Allow downloading camel-catalog over the internet", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

//    @Ignore
//    public void testShouldContainScanThirdPartyComponentsCatalogCheckBox() {
//        JBCheckBox checkBox = editorSettingsPage.getScanThirdPartyComponentsCatalogCheckBox();
//        assertEquals("Scan classpath for third party Camel components using modern component packaging", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }

    public void testShouldContainCamelIconInGutterCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getCamelIconInGutterCheckBox();
        assertEquals("Show Camel icon in gutter", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldDownloadCatalogCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getDownloadCatalogCheckBox();
        checkBox.setSelected(false);
        editorSettingsPage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldResetScanThirdPartyComponentsCatalogCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getScanThirdPartyComponentsCatalogCheckBox();
        checkBox.setSelected(false);
        editorSettingsPage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldResetCamelIconInGutterCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getCamelIconInGutterCheckBox();
        checkBox.setSelected(false);
        editorSettingsPage.reset();
        assertTrue(checkBox.isSelected());
    }

}
