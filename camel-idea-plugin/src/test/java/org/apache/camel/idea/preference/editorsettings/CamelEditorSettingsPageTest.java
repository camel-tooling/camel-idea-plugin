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
package org.apache.camel.idea.preference.editorsettings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

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
            int indexOfApplicationConfigurable = trimmedStrings.indexOf("<applicationConfigurable parentId=\"camel\" id=\"camel.misc\" "
                    + "groupId=\"language\" displayName=\"Miscellaneous\" "
                    + "instance=\"org.apache.camel.idea.preference.editorsettings.CamelEditorSettingsPage\"/>");
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

    public void testShouldContainScanThirdPartyComponentsCatalogCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getScanThirdPartyComponentsCatalogCheckBox();
        assertEquals("Scan classpath for third party Camel components using modern component packaging", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainScanThirdPartyLegacyComponentsCatalogCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getScanThirdPartyLegacyComponentsCatalogCheckBox();
        assertEquals("Scan classpath for third party Camel components using legacy component packaging", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainCamelIconInGutterCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getCamelIconInGutterCheckBox();
        assertEquals("Show Camel icon in gutter", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainCamelIconsComboBox() {
        JComboBox<String> comboBox = editorSettingsPage.getCamelIconsComboBox();
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Icon", comboBox.getSelectedItem());
        assertEquals(4, comboBox.getItemCount());
        assertEquals("Camel Icon", comboBox.getItemAt(0));
        assertEquals("Camel Animal Icon", comboBox.getItemAt(1));
        assertEquals("Camel Badge Icon", comboBox.getItemAt(2));
        assertEquals("Custom Icon", comboBox.getItemAt(3));
        assertEquals(0, comboBox.getSelectedIndex());
    }

    public void testCustomIconButtonShouldNotBeEnabledByDefault() {
        TextFieldWithBrowseButton button = editorSettingsPage.getCustomIconButton();
        assertEquals(false, button.isEnabled());
    }

    public void testCustomIconButtonShouldBeEnabledWhenSelectingCustomCamelIcon() {
        TextFieldWithBrowseButton button = editorSettingsPage.getCustomIconButton();
        JComboBox<String> comboBox = editorSettingsPage.getCamelIconsComboBox();
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Icon", comboBox.getSelectedItem());
        assertEquals(false, button.isEnabled());
        comboBox.setSelectedIndex(3);
        assertEquals(3, comboBox.getSelectedIndex());
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Custom Icon", comboBox.getSelectedItem().toString());
        assertEquals(true, button.isEnabled());
    }

    public void testCustomIconButtonShouldBeEnabledWhenSelectedItemIsCustomIconOnly() {
        TextFieldWithBrowseButton button = editorSettingsPage.getCustomIconButton();
        JComboBox<String> comboBox = editorSettingsPage.getCamelIconsComboBox();

        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Icon", comboBox.getSelectedItem().toString());
        assertEquals(false, button.isEnabled());

        comboBox.setSelectedIndex(1);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Animal Icon", comboBox.getSelectedItem().toString());
        assertEquals(false, button.isEnabled());

        comboBox.setSelectedIndex(2);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Badge Icon", comboBox.getSelectedItem().toString());
        assertEquals(false, button.isEnabled());

        comboBox.setSelectedIndex(3);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Custom Icon", comboBox.getSelectedItem().toString());
        assertEquals(true, button.isEnabled());

        comboBox.setSelectedIndex(1);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Animal Icon", comboBox.getSelectedItem().toString());
        assertEquals(false, button.isEnabled());
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

    public void testShouldResetScanThirdPartyLegacyComponentsCatalogCheckBox() {
        JBCheckBox checkBox = editorSettingsPage.getScanThirdPartyLegacyComponentsCatalogCheckBox();
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

    public void testShouldResetCamelIconsComboBox() {
        JComboBox<String> comboBox = editorSettingsPage.getCamelIconsComboBox();
        comboBox.setSelectedIndex(1);
        editorSettingsPage.reset();
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Icon", comboBox.getSelectedItem().toString());
    }

    public void testResetCustomIconButton() {
        TextFieldWithBrowseButton button = editorSettingsPage.getCustomIconButton();
        JComboBox<String> comboBox = editorSettingsPage.getCamelIconsComboBox();

        comboBox.setSelectedIndex(1);
        assertEquals(false, button.isEnabled());
        editorSettingsPage.reset();
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Icon", comboBox.getSelectedItem().toString());
    }

}