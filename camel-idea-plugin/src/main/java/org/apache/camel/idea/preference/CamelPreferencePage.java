/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import com.intellij.ui.components.JBCheckBox;
import net.miginfocom.swing.MigLayout;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Preference UI for this plugin.
 */
public class CamelPreferencePage implements Configurable {

    private JBCheckBox downloadCatalogCheckBox;

    public CamelPreferencePage() {
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        downloadCatalogCheckBox = new JBCheckBox("Allow downloading camel-catalog over the internet");

        JPanel panel = new JPanel(new MigLayout("fillx,wrap 2",
            "[left]rel[grow,fill]"));
        panel.setOpaque(false);

        panel.add(downloadCatalogCheckBox, "span 2");

        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);

        reset();

        return result;
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
        return getCamelPreferenceService().isDownloadCatalog() != downloadCatalogCheckBox.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        getCamelPreferenceService().setDownloadCatalog(downloadCatalogCheckBox.isSelected());
    }

    @Override
    public void reset() {
        downloadCatalogCheckBox.setSelected(getCamelPreferenceService().isDownloadCatalog());
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

}
