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
import java.util.*;
import java.util.List;
import javax.swing.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import net.miginfocom.swing.MigLayout;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preference UI for this plugin.
 */
public class CamelPreferencePage implements SearchableConfigurable, Configurable.NoScroll {

    private JBCheckBox realTimeEndpointValidationCatalogCheckBox;
    private JBCheckBox realTimeSimpleValidationCatalogCheckBox;
    private JBCheckBox highlightCustomOptionsCheckBox;
    private JBCheckBox downloadCatalogCheckBox;
    private JBCheckBox scanThirdPartyComponentsCatalogCheckBox;
    private JBCheckBox scanThirdPartyLegacyComponentsCatalogCheckBox;
    private JBCheckBox camelIconInGutterCheckBox;
    private JComboBox<String> camelIconsComboBox;
    private TextFieldWithBrowseButton customIconButton;
    private CamelIgnorePropertyTable ignorePropertyTable;
    private CamelExcludePropertyFileTable excludePropertyFileTable;

    public CamelPreferencePage() {
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        realTimeEndpointValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel endpoints in editor");
        realTimeSimpleValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel simple language in editor");
        highlightCustomOptionsCheckBox = new JBCheckBox("Highlight custom endpoint options as warnings in editor");
        downloadCatalogCheckBox = new JBCheckBox("Allow downloading camel-catalog over the internet");
        scanThirdPartyComponentsCatalogCheckBox = new JBCheckBox("Scan classpath for third party Camel components using modern component packaging");
        scanThirdPartyLegacyComponentsCatalogCheckBox = new JBCheckBox("Scan classpath for third party Camel components using legacy component packaging");
        camelIconInGutterCheckBox = new JBCheckBox("Show Camel icon in gutter");
        camelIconsComboBox = new ComboBox<>(new String[] {"Default Icon", "Alternative Icon", "Custom Icon"});
        customIconButton = new TextFieldWithBrowseButton();
        customIconButton.addBrowseFolderListener("Choose Custom Camel Icon", "The icon should be a 16x16 png file", null, FileChooserDescriptorFactory.createSingleFileDescriptor("png"));

        camelIconsComboBox.setRenderer(new CamelChosenIconCellRender(customIconButton));
        camelIconsComboBox.addItemListener((l) -> {
            // only enable custom if selected in the drop down
            customIconButton.setEnabled("Custom Icon".equals(l.getItem()));
        });

        // use mig layout which is like a spread-sheet with 2 columns, which we can span if we only have one element
        JPanel panel = new JPanel(new MigLayout("fillx,wrap 2", "[left]rel[grow,fill]"));
        panel.setOpaque(false);

        panel.add(realTimeEndpointValidationCatalogCheckBox, "span 2");
        panel.add(realTimeSimpleValidationCatalogCheckBox, "span 2");
        panel.add(highlightCustomOptionsCheckBox, "span 2");
        panel.add(downloadCatalogCheckBox, "span 2");
        panel.add(scanThirdPartyComponentsCatalogCheckBox, "span 2");
        panel.add(scanThirdPartyLegacyComponentsCatalogCheckBox, "span 2");
        panel.add(camelIconInGutterCheckBox, "span 2");

        panel.add(new JLabel("Camel icon"));
        panel.add(camelIconsComboBox);

        panel.add(new JLabel("Custom icon file path"));
        panel.add(customIconButton);

        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);
        JPanel propertyTablePanel = new JPanel(new VerticalLayout(1));
        propertyTablePanel.add(createPropertyIgnoreTable(), -1);
        propertyTablePanel.add(createExcludePropertyFilesTable(), -1);
        result.add(propertyTablePanel, -1);
        reset();
        return result;
    }

    private JPanel createPropertyIgnoreTable() {
        final JPanel mainPanel = new JPanel(new GridLayout(1, 1));
        mainPanel.setPreferredSize(JBUI.size(300, 200));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

        ignorePropertyTable = new CamelIgnorePropertyTable(new CamelIgnorePropertyModel(getCamelPreferenceService().getIgnorePropertyList())) {
            @Override
            protected void apply(@NotNull java.util.List<CamelIgnorePropertyModel> configurations) {
                final java.util.List<CamelIgnorePropertyModel> copied = new ArrayList<>();
                try {
                    for (final CamelIgnorePropertyModel configuration : configurations) {
                        copied.add(configuration.clone());
                    }
                } catch (CloneNotSupportedException e) {
                    // ignore
                }
            }
        };

        final JPanel ignorePropertyCamelpanel = ToolbarDecorator.createDecorator(ignorePropertyTable).createPanel();
        final JPanel localPanel = new JPanel(new BorderLayout());
        localPanel.setBorder(IdeBorderFactory.createTitledBorder("Property ignore list", false));
        localPanel.add(ignorePropertyCamelpanel, BorderLayout.CENTER);
        mainPanel.add(localPanel);
        return mainPanel;
    }

    private JPanel createExcludePropertyFilesTable() {
        final JPanel mainPanel = new JPanel(new GridLayout(1, 1));
        mainPanel.setPreferredSize(JBUI.size(300, 200));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

        excludePropertyFileTable = new CamelExcludePropertyFileTable(new CamelExcludePropertyFileModel(getCamelPreferenceService().getExludePropertyFiles())) {
            @Override
            protected void apply(@NotNull List<CamelExcludePropertyFileModel> configurations) {
                final java.util.List<CamelExcludePropertyFileModel> copied = new ArrayList<>();
                try {
                    for (final CamelExcludePropertyFileModel configuration : configurations) {
                        copied.add(configuration.clone());
                    }
                } catch (CloneNotSupportedException e) {
                    // ignore
                }
            }
        };

        final JPanel excludePropertyFilesPanel = ToolbarDecorator.createDecorator(excludePropertyFileTable).createPanel();
        final JPanel localPanel = new JPanel(new BorderLayout());
        localPanel.setBorder(IdeBorderFactory.createTitledBorder("Exclude property file list", false));
        localPanel.add(excludePropertyFilesPanel, BorderLayout.CENTER);
        mainPanel.add(localPanel);
        return mainPanel;
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
        // check boxes
        boolean b1 = getCamelPreferenceService().isRealTimeEndpointValidation() != realTimeEndpointValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isRealTimeSimpleValidation() != realTimeSimpleValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isHighlightCustomOptions() != highlightCustomOptionsCheckBox.isSelected()
            || getCamelPreferenceService().isDownloadCatalog() != downloadCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isScanThirdPartyComponents() != scanThirdPartyComponentsCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isScanThirdPartyLegacyComponents() != scanThirdPartyLegacyComponentsCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isShowCamelIconInGutter() != camelIconInGutterCheckBox.isSelected();

        // other fields
        boolean b2 = !Objects.equals(getCamelPreferenceService().getChosenCamelIcon(), camelIconsComboBox.getSelectedItem())
            || !Objects.equals(getCamelPreferenceService().getCustomIconFilePath(), customIconButton.getText());

        boolean isIgnorePropertiesModified = ignorePropertyTable.isModified();
        boolean isExcludedPropertyFilesModified = excludePropertyFileTable.isModified();

        return b1 || b2 || isIgnorePropertiesModified || isExcludedPropertyFilesModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        getCamelPreferenceService().setRealTimeEndpointValidation(realTimeEndpointValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setRealTimeSimpleValidation(realTimeSimpleValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setHighlightCustomOptions(highlightCustomOptionsCheckBox.isSelected());
        getCamelPreferenceService().setDownloadCatalog(downloadCatalogCheckBox.isSelected());
        getCamelPreferenceService().setScanThirdPartyComponents(scanThirdPartyComponentsCatalogCheckBox.isSelected());
        getCamelPreferenceService().setScanThirdPartyLegacyComponents(scanThirdPartyLegacyComponentsCatalogCheckBox.isSelected());
        getCamelPreferenceService().setShowCamelIconInGutter(camelIconInGutterCheckBox.isSelected());
        getCamelPreferenceService().setChosenCamelIcon(camelIconsComboBox.getSelectedItem().toString());
        getCamelPreferenceService().setCustomIconFilePath(customIconButton.getText());
        getCamelPreferenceService().setIgnorePropertyList(ignorePropertyTable.getIgnoredProperties());
        getCamelPreferenceService().setExludePropertyFiles(excludePropertyFileTable.getExcludePropertyFiles());
    }

    @Override
    public void reset() {
        realTimeEndpointValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeEndpointValidation());
        realTimeSimpleValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeSimpleValidation());
        highlightCustomOptionsCheckBox.setSelected(getCamelPreferenceService().isHighlightCustomOptions());
        downloadCatalogCheckBox.setSelected(getCamelPreferenceService().isDownloadCatalog());
        scanThirdPartyComponentsCatalogCheckBox.setSelected(getCamelPreferenceService().isScanThirdPartyComponents());
        scanThirdPartyLegacyComponentsCatalogCheckBox.setSelected(getCamelPreferenceService().isScanThirdPartyLegacyComponents());
        camelIconInGutterCheckBox.setSelected(getCamelPreferenceService().isShowCamelIconInGutter());
        camelIconsComboBox.setSelectedItem(getCamelPreferenceService().getChosenCamelIcon());
        customIconButton.setText(getCamelPreferenceService().getCustomIconFilePath());
        customIconButton.setEnabled("Custom Icon".equals(camelIconsComboBox.getSelectedItem()));
        ignorePropertyTable.reset();
        excludePropertyFileTable.reset();
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.CamelConfigurable";
    }
}
