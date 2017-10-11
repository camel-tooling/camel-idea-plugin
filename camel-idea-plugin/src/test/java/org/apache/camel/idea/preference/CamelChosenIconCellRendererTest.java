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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;

public class CamelChosenIconCellRendererTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private CamelPreferencePage camelPreferencePage = new CamelPreferencePage();

    public void testPluginXmlShouldContainPreferencesPage() {
        File pluginXml = new File("src/main/resources/META-INF/plugin.xml");
        assertNotNull(pluginXml);

        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/META-INF/plugin.xml"), StandardCharsets.UTF_8);
            List<String> trimmedStrings =
                    lines.stream().map(String::trim).collect(Collectors.toList());
            int indexOfApplicationConfigurable = trimmedStrings.indexOf("<applicationConfigurable id=\"camel\" displayName=\"Apache Camel\" groupId=\"language\"");

            assertTrue(indexOfApplicationConfigurable > 0);
            assertEquals("<applicationConfigurable id=\"camel\" displayName=\"Apache Camel\" groupId=\"language\"", trimmedStrings.get(indexOfApplicationConfigurable));
            String secondLineOfApplicationConfigurable = trimmedStrings.get(indexOfApplicationConfigurable + 1);
            assertEquals("instance=\"org.apache.camel.idea.preference.CamelPreferencePage\"/>", secondLineOfApplicationConfigurable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testDisplayNameShouldBeApacheCamel() {
        assertEquals("Apache Camel", camelPreferencePage.getDisplayName());
    }

    public void testHelpTopicShouldBeNull() {
        assertNull(camelPreferencePage.getHelpTopic());
    }

    public void testPreferencePageIdShouldBeCamelConfigurable() {
        assertEquals("preference.CamelConfigurable", camelPreferencePage.getId());
    }

    public void testShouldContainRealTimeEndpointValidationCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getRealTimeEndpointValidationCatalogCheckBox();
        assertEquals("Real time validation of Camel endpoints in editor", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainRealTimeSimpleValidationCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getRealTimeSimpleValidationCatalogCheckBox();
        assertEquals("Real time validation of Camel simple language in editor", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainHighlightCustomOptionsCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getHighlightCustomOptionsCheckBox();
        assertEquals("Highlight custom endpoint options as warnings in editor", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainDownloadCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getDownloadCatalogCheckBox();
        assertEquals("Allow downloading camel-catalog over the internet", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainScanThirdPartyComponentsCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyComponentsCatalogCheckBox();
        assertEquals("Scan classpath for third party Camel components using modern component packaging", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainScanThirdPartyLegacyComponentsCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyLegacyComponentsCatalogCheckBox();
        assertEquals("Scan classpath for third party Camel components using legacy component packaging", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainCamelIconInGutterCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getCamelIconInGutterCheckBox();
        assertEquals("Show Camel icon in gutter", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainCamelIconsComboBox() {
        camelPreferencePage.createComponent();
        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
        assertEquals(3, comboBox.getItemCount());
        assertEquals("Camel Icon", comboBox.getItemAt(0));
        assertEquals("Camel Badge Icon", comboBox.getItemAt(1));
        assertEquals("Custom Icon", comboBox.getItemAt(2));
        assertEquals(0, comboBox.getSelectedIndex());
    }

    public void testCustomIconButtonShouldNotBeEnabledByDefault() {
        camelPreferencePage.createComponent();
        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
        assertEquals(false, button.isEnabled());
    }

    public void testCustomIconButtonShouldBeEnabledWhenSelectingCustomCamelIcon() {
        camelPreferencePage.createComponent();
        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
        assertEquals("Camel Icon", comboBox.getItemAt(0));
        assertEquals(false, button.isEnabled());
        comboBox.setSelectedIndex(2);
        assertEquals(2, comboBox.getSelectedIndex());
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Custom Icon", comboBox.getSelectedItem().toString());
        assertEquals(true, button.isEnabled());
    }

    public void testCustomIconButtonShouldBeEnabledWhenSelectedItemIsCustomIconOnly() {
        camelPreferencePage.createComponent();
        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();

        assertEquals("Camel Icon", comboBox.getItemAt(0));
        assertEquals(false, button.isEnabled());

        comboBox.setSelectedIndex(1);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Badge Icon", comboBox.getSelectedItem().toString());
        assertEquals(false, button.isEnabled());

        comboBox.setSelectedIndex(2);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Custom Icon", comboBox.getSelectedItem().toString());
        assertEquals(true, button.isEnabled());

        comboBox.setSelectedIndex(1);
        assertNotNull(comboBox.getSelectedItem());
        assertEquals("Camel Badge Icon", comboBox.getSelectedItem().toString());
        assertEquals(false, button.isEnabled());
    }

    public void testShouldContainIgnorePropertyTable() {
        camelPreferencePage.createComponent();
        CamelIgnorePropertyTable table = camelPreferencePage.getIgnorePropertyTable();
        List<String> ignoredProperties = table.getIgnoredProperties();

        String[] strings =  {
            "java.",
            "Logger.",
            "logger",
            "appender.",
            "rootLogger.",
            "camel.springboot.",
            "camel.component.",
            "camel.dataformat.",
            "camel.language."
        };
        List<String> expectedIgnoredProperties = Arrays.asList(strings);
        assertEquals(expectedIgnoredProperties, ignoredProperties);
    }

    public void testShouldResetRealTimeEndpointValidationCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getRealTimeEndpointValidationCatalogCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldRestRealTimeSimpleValidationCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getRealTimeSimpleValidationCatalogCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldResetHighlightCustomOptionsCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getHighlightCustomOptionsCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldDownloadCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getDownloadCatalogCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldResetScanThirdPartyComponentsCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyComponentsCatalogCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldResetScanThirdPartyLegacyComponentsCatalogCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyLegacyComponentsCatalogCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldResetCamelIconInGutterCheckBox() {
        camelPreferencePage.createComponent();
        JBCheckBox checkBox = camelPreferencePage.getCamelIconInGutterCheckBox();
        checkBox.setSelected(false);
        camelPreferencePage.reset();
        assertTrue(checkBox.isSelected());
    }
}
