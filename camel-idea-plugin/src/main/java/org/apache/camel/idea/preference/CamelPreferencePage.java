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

import java.awt.*;
import javax.swing.*;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Preference UI for this plugin.
 */
public class CamelPreferencePage implements Configurable {

    private JBCheckBox realTimeEndpointValidationCatalogCheckBox;
    private JBCheckBox realTimeSimpleValidationCatalogCheckBox;
    private JBCheckBox downloadCatalogCheckBox;
    private JBCheckBox scanThirdPartyComponentsCatalogCheckBox;
    private JBCheckBox scanThirdPartyLegacyComponentsCatalogCheckBox;
    private JBCheckBox camelIconInGutterCheckBox;

    public CamelPreferencePage() {
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        realTimeEndpointValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel endpoints in editor");
        realTimeSimpleValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel simple language in editor");
        downloadCatalogCheckBox = new JBCheckBox("Allow downloading camel-catalog over the internet");
        scanThirdPartyComponentsCatalogCheckBox = new JBCheckBox("Scan for third party Camel components using modern component packaging");
        scanThirdPartyLegacyComponentsCatalogCheckBox = new JBCheckBox("Scan the classpath for third party Camel components using legacy component packaging");
        camelIconInGutterCheckBox = new JBCheckBox("Show Camel icon in gutter");

        JPanel c = new JPanel(new BorderLayout());

        JPanel settings = new JPanel(new BorderLayout());
        settings.setBorder(IdeBorderFactory.createTitledBorder("Settings", true));
        c.add(c = new JPanel(new BorderLayout()), BorderLayout.NORTH);
        c.add(settings, BorderLayout.NORTH);

        settings.add(realTimeEndpointValidationCatalogCheckBox, BorderLayout.NORTH);
        settings.add(settings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);
        settings.add(realTimeSimpleValidationCatalogCheckBox, BorderLayout.NORTH);
        settings.add(settings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);
        settings.add(downloadCatalogCheckBox, BorderLayout.NORTH);
        settings.add(settings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);
        settings.add(scanThirdPartyComponentsCatalogCheckBox, BorderLayout.NORTH);
        settings.add(settings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);
        settings.add(scanThirdPartyLegacyComponentsCatalogCheckBox, BorderLayout.NORTH);
        settings.add(settings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);
        settings.add(camelIconInGutterCheckBox, BorderLayout.NORTH);
        settings.add(settings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);

        reset();

        return c;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Apache Camel";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public boolean isModified() {
        return getCamelPreferenceService().isRealTimeEndpointValidation() != realTimeEndpointValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isRealTimeSimpleValidation() != realTimeSimpleValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isDownloadCatalog() != downloadCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isScanThirdPartyComponents() != scanThirdPartyComponentsCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isScanThirdPartyLegacyComponents() != scanThirdPartyLegacyComponentsCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isShowCamelIconInGutter() != camelIconInGutterCheckBox.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        getCamelPreferenceService().setRealTimeEndpointValidation(realTimeEndpointValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setRealTimeSimpleValidation(realTimeSimpleValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setDownloadCatalog(downloadCatalogCheckBox.isSelected());
        getCamelPreferenceService().setScanThirdPartyComponents(scanThirdPartyComponentsCatalogCheckBox.isSelected());
        getCamelPreferenceService().setScanThirdPartyLegacyComponents(scanThirdPartyLegacyComponentsCatalogCheckBox.isSelected());
        getCamelPreferenceService().setShowCamelIconInGutter(camelIconInGutterCheckBox.isSelected());
    }

    @Override
    public void reset() {
        realTimeEndpointValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeEndpointValidation());
        realTimeSimpleValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeSimpleValidation());
        downloadCatalogCheckBox.setSelected(getCamelPreferenceService().isDownloadCatalog());
        scanThirdPartyComponentsCatalogCheckBox.setSelected(getCamelPreferenceService().isScanThirdPartyComponents());
        scanThirdPartyLegacyComponentsCatalogCheckBox.setSelected(getCamelPreferenceService().isScanThirdPartyLegacyComponents());
        camelIconInGutterCheckBox.setSelected(getCamelPreferenceService().isShowCamelIconInGutter());
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

}
