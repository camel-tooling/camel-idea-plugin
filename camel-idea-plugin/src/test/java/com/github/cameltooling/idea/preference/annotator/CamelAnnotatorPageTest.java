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
package com.github.cameltooling.idea.preference.annotator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;

public class CamelAnnotatorPageTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private CamelAnnotatorPage annotatorPage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        annotatorPage = new CamelAnnotatorPage();
        annotatorPage.createComponent();
        super.initCamelPreferencesService();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        annotatorPage.disposeUIResources();
        annotatorPage = null;
    }

    public void testPluginXmlShouldContainAnnotatorPreferencesPage() {
        File pluginXml = new File("src/main/resources/META-INF/plugin.xml");
        assertNotNull(pluginXml);

        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/META-INF/plugin.xml"), StandardCharsets.UTF_8);
            List<String> trimmedStrings =
                    lines.stream().map(String::trim).collect(Collectors.toList());
            int indexOfApplicationConfigurable = trimmedStrings.indexOf("<applicationConfigurable parentId=\"camel\" id=\"camel.annotator\" "
                    + "groupId=\"language\" displayName=\"Validation\" "
                    + "instance=\"com.github.cameltooling.idea.preference.annotator.CamelAnnotatorPage\"/>");
            assertTrue(indexOfApplicationConfigurable > 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testShouldContainRealTimeEndpointValidationCatalogCheckBox() {
        JBCheckBox checkBox = annotatorPage.getRealTimeEndpointValidationCatalogCheckBox();
        assertEquals("Real time validation of Camel endpoints in editor", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainRealTimeSimpleValidationCatalogCheckBox() {
        JBCheckBox checkBox = annotatorPage.getRealTimeSimpleValidationCatalogCheckBox();
        assertEquals("Real time validation of Camel simple language in editor", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldContainHighlightCustomOptionsCheckBox() {
        JBCheckBox checkBox = annotatorPage.getHighlightCustomOptionsCheckBox();
        assertEquals("Highlight custom endpoint options as warnings in editor", checkBox.getText());
        assertTrue(checkBox.isSelected());
    }

    public void testShouldChangeStateOfRealTimeEndpointValidationCatalogCheckBox() throws ConfigurationException {
        JBCheckBox checkBox = annotatorPage.getRealTimeEndpointValidationCatalogCheckBox();
        assertEquals(true, checkBox.isSelected());
        assertEquals(true, annotatorPage.getCamelPreferenceService().isRealTimeEndpointValidation());
        checkBox.setSelected(false);
        annotatorPage.apply();
        assertEquals(false, checkBox.isSelected());
        assertEquals(false, annotatorPage.getCamelPreferenceService().isRealTimeEndpointValidation());
    }

    public void testShouldChangeStateOfRealTimeSimpleValidationCatalogCheckBox() throws ConfigurationException {
        JBCheckBox checkBox = annotatorPage.getRealTimeSimpleValidationCatalogCheckBox();
        assertEquals(true, checkBox.isSelected());
        assertEquals(true, annotatorPage.getCamelPreferenceService().isRealTimeSimpleValidation());
        checkBox.setSelected(false);
        annotatorPage.apply();
        assertEquals(false, checkBox.isSelected());
        assertEquals(false, annotatorPage.getCamelPreferenceService().isRealTimeSimpleValidation());
    }

    public void testShouldChangeStateOfHighlightCustomOptionsCheckBox() throws ConfigurationException {
        JBCheckBox checkBox = annotatorPage.getHighlightCustomOptionsCheckBox();
        assertEquals(true, checkBox.isSelected());
        assertEquals(true, annotatorPage.getCamelPreferenceService().isHighlightCustomOptions());
        checkBox.setSelected(false);
        annotatorPage.apply();
        assertEquals(false, checkBox.isSelected());
        assertEquals(false, annotatorPage.getCamelPreferenceService().isHighlightCustomOptions());
    }

    public void testShouldResetRealTimeEndpointValidationCatalogCheckBox() {
        JBCheckBox checkBox = annotatorPage.getRealTimeEndpointValidationCatalogCheckBox();
        checkBox.setSelected(false);
        annotatorPage.reset();
        assertTrue(checkBox.isSelected());
    }

    public void testShouldRestRealTimeSimpleValidationCatalogCheckBox() {
        JBCheckBox checkBox = annotatorPage.getRealTimeSimpleValidationCatalogCheckBox();
        boolean status = checkBox.isSelected();
        checkBox.setSelected(!status);
        annotatorPage.reset();
        assertEquals(status, checkBox.isSelected());
    }

    public void testShouldResetHighlightCustomOptionsCheckBox() {
        JBCheckBox checkBox = annotatorPage.getHighlightCustomOptionsCheckBox();
        boolean status = checkBox.isSelected();
        checkBox.setSelected(!status);
        annotatorPage.reset();
        assertEquals(status, checkBox.isSelected());
    }

    public void testShouldChangeStateOfRealTimeJSonPathValidationCatalogCheckBox() {
        JBCheckBox checkBox = annotatorPage.getRealTimeJSonPathValidationCatalogCheckBox();
        assertEquals(true, checkBox.isSelected());
        assertEquals(true, annotatorPage.getCamelPreferenceService().isRealTimeJSonPathValidation());
        checkBox.setSelected(false);
        annotatorPage.apply();
        assertEquals(false, checkBox.isSelected());
        assertEquals(false, annotatorPage.getCamelPreferenceService().isRealTimeJSonPathValidation());
    }

    public void testShouldChangeStateOfRealTimeBeanMethodValidationCheckBox() {
        JBCheckBox checkBox = annotatorPage.getRealTimeBeanMethodValidationCheckBox();
        assertEquals(true, checkBox.isSelected());
        assertEquals(true, annotatorPage.getCamelPreferenceService().isRealTimeBeanMethodValidationCheckBox());
        checkBox.setSelected(false);
        annotatorPage.apply();
        assertEquals(false, checkBox.isSelected());
        assertEquals(false, annotatorPage.getCamelPreferenceService().isRealTimeBeanMethodValidationCheckBox());
    }

}
