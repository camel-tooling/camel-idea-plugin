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
package org.apache.camel.idea.preference.properties;

import java.awt.*;
import java.util.Objects;
import javax.swing.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelIgnoreAndExcludePage extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private AddEditRemovePanel<String> excludePropertyFilePanel;
    private AddEditRemovePanel<String> ignorePropertyFilePanel;

    public CamelIgnoreAndExcludePage() {
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.CamelConfigurable";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Ignore and exclude properties";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel result = new JPanel(new BorderLayout());
        JPanel propertyTablePanel = new JPanel(new VerticalLayout(1));
        propertyTablePanel.add(createIgnorePropertiesFilesTable());
        propertyTablePanel.add(createExcludePropertiesFilesTable());
        result.add(propertyTablePanel);

        reset();

        return result;
    }

//    @Override
//    public boolean isModified() {
//        return !Objects.equals(getCamelPreferenceService().getExcludePropertyFiles(), excludePropertyFilePanel.getData())
//                || !Objects.equals(getCamelPreferenceService().getIgnorePropertyList(), ignorePropertyFilePanel.getData());
//    }

    @Override
    public void apply() throws ConfigurationException {
        getCamelPreferenceService().setExcludePropertyFiles(excludePropertyFilePanel.getData());
        getCamelPreferenceService().setIgnorePropertyList(ignorePropertyFilePanel.getData());

        setModified(false);
    }

    @Override
    public void reset() {
        excludePropertyFilePanel.setData(getCamelPreferenceService().getExcludePropertyFiles());
        ignorePropertyFilePanel.setData(getCamelPreferenceService().getIgnorePropertyList());

        //setModified(false);
        //setModified(!myNewPairs.isEmpty());

//        boolean b = !Objects.equals(getCamelPreferenceService().getExcludePropertyFiles(), excludePropertyFilePanel.getData())
//                || !Objects.equals(getCamelPreferenceService().getIgnorePropertyList(), ignorePropertyFilePanel.getData());
//        setModified(!b);
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

    private JPanel createExcludePropertiesFilesTable() {
        final JPanel mainPanel = new JPanel(new GridLayout(1, 1));
        mainPanel.setPreferredSize(JBUI.size(300, 300));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        excludePropertyFilePanel = new AddEditRemovePanel<String>(new IgnoredUrlsModel(), getCamelPreferenceService().getExcludePropertyFiles(), "Exclude property file list") {
            @Override
            protected String addItem() {
                return addIgnoreLocation();
            }

            @Override
            protected boolean removeItem(String o) {
                setModified(true);
                return true;
            }

            @Override
            protected String editItem(String o) {
                return editIgnoreLocation(o);
            }
        };
        mainPanel.add(excludePropertyFilePanel);
        excludePropertyFilePanel.setData(getCamelPreferenceService().getExcludePropertyFiles());

        return mainPanel;
    }

    private JPanel createIgnorePropertiesFilesTable() {
        final JPanel mainPanel = new JPanel(new GridLayout(1, 1));
        mainPanel.setPreferredSize(JBUI.size(300, 300));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        ignorePropertyFilePanel = new AddEditRemovePanel<String>(new IgnoredUrlsModel(), getCamelPreferenceService().getExcludePropertyFiles(), "Property ignore list") {
            @Override
            protected String addItem() {
                return addIgnoreLocation();
            }

            @Override
            protected boolean removeItem(String o) {
                setModified(true);
                return true;
            }

            @Override
            protected String editItem(String o) {
                return editIgnoreLocation(o);
            }
        };
        mainPanel.add(ignorePropertyFilePanel);
        ignorePropertyFilePanel.setData(getCamelPreferenceService().getIgnorePropertyList());

        return mainPanel;
    }

    @Nullable
    private String addIgnoreLocation() {
        CamelEditDialog dialog = new CamelEditDialog(null);
        if (!dialog.showAndGet()) {
            return null;
        }
        setModified(true);
        return dialog.getTextFieldText();
    }

    @Nullable
    private String editIgnoreLocation(Object o) {
        CamelEditDialog dialog = new CamelEditDialog(null);
        dialog.init(o.toString());
        if (!dialog.showAndGet()) {
            return null;
        }
        setModified(true);
        return dialog.getTextFieldText();
    }

    private static class IgnoredUrlsModel extends AddEditRemovePanel.TableModel<String> {

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isEditable(int column) {
            return false;
        }

        @Override
        public void setValue(Object aValue, String data, int columnIndex) {

        }

        @Override
        public String getColumnName(int column) {
            return "Filename";
        }
    }
}
