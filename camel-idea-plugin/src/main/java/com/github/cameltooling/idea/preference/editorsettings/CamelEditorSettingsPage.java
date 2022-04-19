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

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.components.JBCheckBox;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CamelEditorSettingsPage extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private JBCheckBox downloadCatalogCheckBox;
    private JBCheckBox scanThirdPartyComponentsCatalogCheckBox;
    private JBCheckBox camelIconInGutterCheckBox;
    private JBCheckBox enableDebuggerCheckBox;

    @Nullable
    @Override
    public JComponent createComponent() {
        downloadCatalogCheckBox = new JBCheckBox("Allow downloading camel-catalog over the internet");
        scanThirdPartyComponentsCatalogCheckBox = new JBCheckBox("Scan classpath for third party Camel components");
        camelIconInGutterCheckBox = new JBCheckBox("Show Camel icon in gutter");
        enableDebuggerCheckBox = new JBCheckBox("Enable Camel Debugger");

        // use mig layout which is like a spread-sheet with 2 columns, which we can span if we only have one element
        JPanel panel = new JPanel(new MigLayout("fillx,wrap 2", "[left]rel[grow,fill]"));
        panel.setOpaque(false);

        panel.add(downloadCatalogCheckBox, "span 2");
        panel.add(scanThirdPartyComponentsCatalogCheckBox, "span 2");
        panel.add(camelIconInGutterCheckBox, "span 2");
        panel.add(enableDebuggerCheckBox, "span 2");

        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);
        reset();
        return result;
    }

    @Override
    public void apply() throws ConfigurationException {
        getCamelPreferenceService().setDownloadCatalog(downloadCatalogCheckBox.isSelected());
        getCamelPreferenceService().setScanThirdPartyComponents(scanThirdPartyComponentsCatalogCheckBox.isSelected());
        getCamelPreferenceService().setShowCamelIconInGutter(camelIconInGutterCheckBox.isSelected());
        getCamelPreferenceService().setEnableCamelDebugger(enableDebuggerCheckBox.isSelected());
    }

    @Override
    public boolean isModified() {
        // check boxes
        boolean b1 = getCamelPreferenceService().isDownloadCatalog() != downloadCatalogCheckBox.isSelected()
                || getCamelPreferenceService().isScanThirdPartyComponents() != scanThirdPartyComponentsCatalogCheckBox.isSelected()
                || getCamelPreferenceService().isShowCamelIconInGutter() != camelIconInGutterCheckBox.isSelected()
                || getCamelPreferenceService().isEnableCamelDebugger() != enableDebuggerCheckBox.isSelected();
        return b1;
    }

    @Override
    public void reset() {
        downloadCatalogCheckBox.setSelected(getCamelPreferenceService().isDownloadCatalog());
        scanThirdPartyComponentsCatalogCheckBox.setSelected(getCamelPreferenceService().isScanThirdPartyComponents());
        camelIconInGutterCheckBox.setSelected(getCamelPreferenceService().isShowCamelIconInGutter());
        enableDebuggerCheckBox.setSelected(getCamelPreferenceService().isEnableCamelDebugger());
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

    CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
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
