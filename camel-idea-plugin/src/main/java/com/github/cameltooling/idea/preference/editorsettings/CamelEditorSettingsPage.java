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

import java.awt.*;

import javax.swing.*;

import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelEditorSettingsPage extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private JBCheckBox downloadCatalogCheckBox;
    private JBCheckBox scanThirdPartyComponentsCatalogCheckBox;
    private JBCheckBox camelIconInGutterCheckBox;
    private JBCheckBox enableDebuggerCheckBox;
    private JBCheckBox camelDebuggerAutoSetupCheckBox;
    private JBCheckBox onlyShowKameletOptionsCheckBox;
    private JComboBox<CamelCatalogProvider> camelRuntimeProviderComboBox;

    @Nullable
    @Override
    public JComponent createComponent() {
        downloadCatalogCheckBox = new JBCheckBox("Allow downloading camel-catalog over the internet");
        scanThirdPartyComponentsCatalogCheckBox = new JBCheckBox("Scan classpath for third party Camel components");
        camelIconInGutterCheckBox = new JBCheckBox("Show Camel icon in gutter");
        enableDebuggerCheckBox = new JBCheckBox("Enable Camel Debugger");
        camelDebuggerAutoSetupCheckBox = new JBCheckBox("Setup automatically the Camel Debugger");
        onlyShowKameletOptionsCheckBox = new JBCheckBox("Only show Kamelet's own options");
        camelRuntimeProviderComboBox = new ComboBox<>(CamelCatalogProvider.values());
        camelRuntimeProviderComboBox.setRenderer(
            new SimpleListCellRenderer<>() {
                @Override
                public void customize(@NotNull JList<? extends CamelCatalogProvider> list, CamelCatalogProvider value,
                                      int index, boolean selected, boolean hasFocus) {
                    this.setText(value.getName());
                }
            }
        );

        // use mig layout which is like a spreadsheet with 2 columns, which we can span if we only have one element
        JPanel panel = new JPanel(new MigLayout("fillx,wrap 2", "[left]rel[grow,fill]"));
        panel.setOpaque(false);

        final String constraintsForCheckBox = "span 2";
        panel.add(downloadCatalogCheckBox, constraintsForCheckBox);
        panel.add(scanThirdPartyComponentsCatalogCheckBox, constraintsForCheckBox);
        panel.add(camelIconInGutterCheckBox, constraintsForCheckBox);
        panel.add(enableDebuggerCheckBox, constraintsForCheckBox);
        panel.add(camelDebuggerAutoSetupCheckBox, constraintsForCheckBox);
        panel.add(onlyShowKameletOptionsCheckBox, constraintsForCheckBox);

        panel.add(new JLabel("Camel Runtime Provider"));
        panel.add(camelRuntimeProviderComboBox);

        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);
        reset();
        return result;
    }

    @Override
    public void apply() throws ConfigurationException {
        final CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        preferenceService.setDownloadCatalog(downloadCatalogCheckBox.isSelected());
        preferenceService.setScanThirdPartyComponents(scanThirdPartyComponentsCatalogCheckBox.isSelected());
        preferenceService.setShowCamelIconInGutter(camelIconInGutterCheckBox.isSelected());
        preferenceService.setEnableCamelDebugger(enableDebuggerCheckBox.isSelected());
        preferenceService.setCamelDebuggerAutoSetup(camelDebuggerAutoSetupCheckBox.isSelected());
        preferenceService.setOnlyShowKameletOptions(onlyShowKameletOptionsCheckBox.isSelected());
        preferenceService.setCamelCatalogProvider((CamelCatalogProvider) camelRuntimeProviderComboBox.getSelectedItem());
    }

    @Override
    public boolean isModified() {
        // check boxes
        final CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        return preferenceService.isDownloadCatalog() != downloadCatalogCheckBox.isSelected()
                || preferenceService.isScanThirdPartyComponents() != scanThirdPartyComponentsCatalogCheckBox.isSelected()
                || preferenceService.isShowCamelIconInGutter() != camelIconInGutterCheckBox.isSelected()
                || preferenceService.isEnableCamelDebugger() != enableDebuggerCheckBox.isSelected()
                || preferenceService.isCamelDebuggerAutoSetup() != camelDebuggerAutoSetupCheckBox.isSelected()
                || preferenceService.isOnlyShowKameletOptions() != onlyShowKameletOptionsCheckBox.isSelected()
                || preferenceService.getCamelCatalogProvider() != camelRuntimeProviderComboBox.getSelectedItem();
    }

    @Override
    public void reset() {
        final CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        downloadCatalogCheckBox.setSelected(preferenceService.isDownloadCatalog());
        scanThirdPartyComponentsCatalogCheckBox.setSelected(preferenceService.isScanThirdPartyComponents());
        camelIconInGutterCheckBox.setSelected(preferenceService.isShowCamelIconInGutter());
        enableDebuggerCheckBox.setSelected(preferenceService.isEnableCamelDebugger());
        camelDebuggerAutoSetupCheckBox.setSelected(preferenceService.isCamelDebuggerAutoSetup());
        onlyShowKameletOptionsCheckBox.setSelected(preferenceService.isOnlyShowKameletOptions());
        camelRuntimeProviderComboBox.setSelectedItem(preferenceService.getCamelCatalogProvider());
    }

    @NotNull
    @Override
    public String getId() {
        return "camel.settings";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return null;
    }

    JBCheckBox getDownloadCatalogCheckBox() {
        return downloadCatalogCheckBox;
    }

    JBCheckBox getScanThirdPartyComponentsCatalogCheckBox() {
        return scanThirdPartyComponentsCatalogCheckBox;
    }

    JBCheckBox getCamelIconInGutterCheckBox() {
        return camelIconInGutterCheckBox;
    }

    public JBCheckBox getEnableDebuggerCheckBox() {
        return enableDebuggerCheckBox;
    }
}
