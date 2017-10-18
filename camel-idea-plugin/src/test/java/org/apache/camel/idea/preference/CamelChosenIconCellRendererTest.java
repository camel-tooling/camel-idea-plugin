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

    private CamelPreferencePage camelPreferencePage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        camelPreferencePage = new CamelPreferencePage();
        camelPreferencePage.createComponent();
        super.initCamelPreferencesService();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        camelPreferencePage = null;
        super.initCamelPreferencesService();
    }
//
//    public void testPluginXmlShouldContainPreferencesPage() {
//        File pluginXml = new File("src/main/resources/META-INF/plugin.xml");
//        assertNotNull(pluginXml);
//
//        try {
//            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/META-INF/plugin.xml"), StandardCharsets.UTF_8);
//            List<String> trimmedStrings =
//                    lines.stream().map(String::trim).collect(Collectors.toList());
//            int indexOfApplicationConfigurable = trimmedStrings.indexOf("<applicationConfigurable id=\"camel\" displayName=\"Apache Camel\" groupId=\"language\"");
//
//            assertTrue(indexOfApplicationConfigurable > 0);
//            assertEquals("<applicationConfigurable id=\"camel\" displayName=\"Apache Camel\" groupId=\"language\"", trimmedStrings.get(indexOfApplicationConfigurable));
//            String secondLineOfApplicationConfigurable = trimmedStrings.get(indexOfApplicationConfigurable + 1);
//            assertEquals("instance=\"org.apache.camel.idea.preference.CamelPreferencePage\"/>", secondLineOfApplicationConfigurable);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void testDisplayNameShouldBeApacheCamel() {
//        assertEquals("Apache Camel", camelPreferencePage.getDisplayName());
//    }
//
//    public void testHelpTopicShouldBeNull() {
//        assertNull(camelPreferencePage.getHelpTopic());
//    }
//
//    public void testPreferencePageIdShouldBeCamelConfigurable() {
//        assertEquals("preference.CamelConfigurable", camelPreferencePage.getId());
//    }
//
//    public void testShouldContainRealTimeEndpointValidationCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getRealTimeEndpointValidationCatalogCheckBox();
//        assertEquals("Real time validation of Camel endpoints in editor", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainRealTimeSimpleValidationCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getRealTimeSimpleValidationCatalogCheckBox();
//        assertEquals("Real time validation of Camel simple language in editor", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainHighlightCustomOptionsCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getHighlightCustomOptionsCheckBox();
//        assertEquals("Highlight custom endpoint options as warnings in editor", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainDownloadCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getDownloadCatalogCheckBox();
//        assertEquals("Allow downloading camel-catalog over the internet", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainScanThirdPartyComponentsCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyComponentsCatalogCheckBox();
//        assertEquals("Scan classpath for third party Camel components using modern component packaging", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainScanThirdPartyLegacyComponentsCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyLegacyComponentsCatalogCheckBox();
//        assertEquals("Scan classpath for third party Camel components using legacy component packaging", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainCamelIconInGutterCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getCamelIconInGutterCheckBox();
//        assertEquals("Show Camel icon in gutter", checkBox.getText());
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldContainCamelIconsComboBox() {
//        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Icon", comboBox.getSelectedItem());
//        assertEquals(3, comboBox.getItemCount());
//        assertEquals("Camel Icon", comboBox.getItemAt(0));
//        assertEquals("Camel Badge Icon", comboBox.getItemAt(1));
//        assertEquals("Custom Icon", comboBox.getItemAt(2));
//        assertEquals(0, comboBox.getSelectedIndex());
//    }
//
//    public void testCustomIconButtonShouldNotBeEnabledByDefault() {
//        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
//        assertEquals(false, button.isEnabled());
//    }
//
//    public void testCustomIconButtonShouldBeEnabledWhenSelectingCustomCamelIcon() {
//        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
//        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Icon", comboBox.getSelectedItem());
//        assertEquals(false, button.isEnabled());
//        comboBox.setSelectedIndex(2);
//        assertEquals(2, comboBox.getSelectedIndex());
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Custom Icon", comboBox.getSelectedItem().toString());
//        assertEquals(true, button.isEnabled());
//    }
//
//    public void testCustomIconButtonShouldBeEnabledWhenSelectedItemIsCustomIconOnly() {
//        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
//        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
//
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Icon", comboBox.getSelectedItem().toString());
//        assertEquals(false, button.isEnabled());
//
//        comboBox.setSelectedIndex(1);
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Badge Icon", comboBox.getSelectedItem().toString());
//        assertEquals(false, button.isEnabled());
//
//        comboBox.setSelectedIndex(2);
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Custom Icon", comboBox.getSelectedItem().toString());
//        assertEquals(true, button.isEnabled());
//
//        comboBox.setSelectedIndex(1);
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Badge Icon", comboBox.getSelectedItem().toString());
//        assertEquals(false, button.isEnabled());
//    }

//    public void testShouldContainIgnorePropertyTable() {
//        CamelIgnorePropertyTable table = camelPreferencePage.getIgnorePropertyTable();
//        List<String> ignoredProperties = table.getIgnoredProperties();
//        List<String> expectedIgnoredProperties = Arrays.asList("java.", "Logger.", "logger", "appender.", "rootLogger.",
//                "camel.springboot.", "camel.component.", "camel.dataformat.", "camel.language.");
//        assertEquals(expectedIgnoredProperties, ignoredProperties);
//    }
//
//    public void testShouldContainExcludePropertyTable() {
//        CamelExcludePropertyFileTable table = camelPreferencePage.getExcludePropertyFileTable();
//        List<String> excludePropertyFiles = table.getExcludePropertyFiles();
//        List<String> expectedExcludedProperties = Arrays.asList("**/log4j.properties", "**/log4j2.properties", "**/logging.properties");
//        assertEquals(expectedExcludedProperties, excludePropertyFiles);
//    }
//
//    public void testShouldResetRealTimeEndpointValidationCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getRealTimeEndpointValidationCatalogCheckBox();
//        checkBox.setSelected(false);
//        camelPreferencePage.reset();
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldRestRealTimeSimpleValidationCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getRealTimeSimpleValidationCatalogCheckBox();
//        boolean status = checkBox.isSelected();
//        checkBox.setSelected(!status);
//        camelPreferencePage.reset();
//        assertEquals(status, checkBox.isSelected());
//    }
//
//    public void testShouldResetHighlightCustomOptionsCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getHighlightCustomOptionsCheckBox();
//        boolean status = checkBox.isSelected();
//        checkBox.setSelected(!status);
//        camelPreferencePage.reset();
//        assertEquals(status, checkBox.isSelected());
//    }
//
//    public void testShouldDownloadCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getDownloadCatalogCheckBox();
//        checkBox.setSelected(false);
//        camelPreferencePage.reset();
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldResetScanThirdPartyComponentsCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyComponentsCatalogCheckBox();
//        checkBox.setSelected(false);
//        camelPreferencePage.reset();
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldResetScanThirdPartyLegacyComponentsCatalogCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getScanThirdPartyLegacyComponentsCatalogCheckBox();
//        checkBox.setSelected(false);
//        camelPreferencePage.reset();
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldResetCamelIconInGutterCheckBox() {
//        JBCheckBox checkBox = camelPreferencePage.getCamelIconInGutterCheckBox();
//        checkBox.setSelected(false);
//        camelPreferencePage.reset();
//        assertTrue(checkBox.isSelected());
//    }
//
//    public void testShouldResetCamelIconsComboBox() {
//        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
//        comboBox.setSelectedIndex(1);
//        camelPreferencePage.reset();
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Icon", comboBox.getSelectedItem().toString());
//    }
//
//    public void testResetCustomIconButton() {
//        TextFieldWithBrowseButton button = camelPreferencePage.getCustomIconButton();
//        JComboBox<String> comboBox = camelPreferencePage.getCamelIconsComboBox();
//
//        comboBox.setSelectedIndex(1);
//        assertEquals(false, button.isEnabled());
//        camelPreferencePage.reset();
//        assertNotNull(comboBox.getSelectedItem());
//        assertEquals("Camel Icon", comboBox.getSelectedItem().toString());
//    }

//    public void testShouldResetIgnorePropertyTable() {
//        CamelIgnorePropertyTable table = camelPreferencePage.getIgnorePropertyTable();
//        assertEquals(9, table.getModel().getRowCount());
//        table.getModel().removeRow(0);
//        assertEquals(8, table.getModel().getRowCount());
//        camelPreferencePage.reset();
//        assertEquals(9, table.getModel().getRowCount());
//    }
//
//    public void testShouldResetExcludePropertyTable() {
//        CamelExcludePropertyFileTable table = camelPreferencePage.getExcludePropertyFileTable();
//        int initialRowCount = table.getModel().getRowCount();
//        table.getModel().removeRow(0);
//        assertEquals(initialRowCount - 1, table.getModel().getRowCount());
//        camelPreferencePage.reset();
//        assertEquals(initialRowCount, table.getModel().getRowCount());
//    }
//
//    public void testShouldChangeStateOfRealTimeEndpointValidationCatalogCheckBox() throws ConfigurationException {
//        JBCheckBox checkBox = camelPreferencePage.getRealTimeEndpointValidationCatalogCheckBox();
//        assertEquals(true, checkBox.isSelected());
//        assertEquals(true, camelPreferencePage.getCamelPreferenceService().isRealTimeEndpointValidation());
//        checkBox.setSelected(false);
//        camelPreferencePage.apply();
//        assertEquals(false, checkBox.isSelected());
//        assertEquals(false, camelPreferencePage.getCamelPreferenceService().isRealTimeEndpointValidation());
//    }
//
//    public void testShouldChangeStateOfRealTimeSimpleValidationCatalogCheckBox() throws ConfigurationException {
//        JBCheckBox checkBox = camelPreferencePage.getRealTimeSimpleValidationCatalogCheckBox();
//        assertEquals(true, checkBox.isSelected());
//        assertEquals(true, camelPreferencePage.getCamelPreferenceService().isRealTimeSimpleValidation());
//        checkBox.setSelected(false);
//        camelPreferencePage.apply();
//        assertEquals(false, checkBox.isSelected());
//        assertEquals(false, camelPreferencePage.getCamelPreferenceService().isRealTimeSimpleValidation());
//    }
//
//    public void testShouldChangeStateOfHighlightCustomOptionsCheckBox() throws ConfigurationException {
//        JBCheckBox checkBox = camelPreferencePage.getHighlightCustomOptionsCheckBox();
//        assertEquals(true, checkBox.isSelected());
//        assertEquals(true, camelPreferencePage.getCamelPreferenceService().isHighlightCustomOptions());
//        checkBox.setSelected(false);
//        camelPreferencePage.apply();
//        assertEquals(false, checkBox.isSelected());
//        assertEquals(false, camelPreferencePage.getCamelPreferenceService().isHighlightCustomOptions());
//    }
}
