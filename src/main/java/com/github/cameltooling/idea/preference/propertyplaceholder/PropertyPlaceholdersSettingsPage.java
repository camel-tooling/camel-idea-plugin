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
package com.github.cameltooling.idea.preference.propertyplaceholder;

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.table.JBTable;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class PropertyPlaceholdersSettingsPage extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll{

    private JPanel root;
    private AddEditRemovePanel<PropertyPlaceholderSettingsEntry> tablePanel;
    private List<PropertyPlaceholderSettingsEntry> data = new ArrayList<>();

    @Override
    public @NotNull @NonNls String getId() {
        return "camel.property-placeholders";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Property Placeholders";
    }

    @Override
    public @Nullable JComponent createComponent() {
        root = new JPanel(new BorderLayout());
        tablePanel = new AddEditRemovePanel<>(new PlaceholderTableModel(), data, "XML property placeholders") {
            @Override
            protected PropertyPlaceholderSettingsEntry addItem() {
                return addOrEdit(null);
            }

            @Override
            protected boolean removeItem(PropertyPlaceholderSettingsEntry o) {
                setModified(true);
                return true;
            }

            @Override
            protected PropertyPlaceholderSettingsEntry editItem(PropertyPlaceholderSettingsEntry o) {
                return addOrEdit(o);
            }

            private PropertyPlaceholderSettingsEntry addOrEdit(PropertyPlaceholderSettingsEntry entry) {
                PropertyPlaceholderEditDialog dialog = new PropertyPlaceholderEditDialog(null);
                if (entry != null) {
                    dialog.initValues(entry);
                }
                if (!dialog.showAndGet()) {
                    return null;
                }
                String start = dialog.getStartToken();
                String end = dialog.getEndToken();
                List<String> namespaces = dialog.getNamespaces();

                if (start.isEmpty() || end.isEmpty()) {
                    // basic validation fails: ignore
                    return null;
                }
                if (namespaces == null || namespaces.isEmpty()) {
                    return null;
                }
                setModified(true);
                return new PropertyPlaceholderSettingsEntry(start, end, namespaces, entry == null || entry.isEnabled());
            }
        };
        JBTable t = tablePanel.getTable();
        t.setShowColumns(true);
        JPanel container = new JPanel(new VerticalLayout(1));
        JPanel tableContainer = new JPanel(new GridLayout(1,1));
        tableContainer.setPreferredSize(JBUI.size(400, 250));
        tableContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        tableContainer.add(tablePanel);
        container.add(tableContainer);
        root.add(container, BorderLayout.CENTER);

        reset();
        return root;
    }

    @Override
    public void apply() throws ConfigurationException {
        CamelPreferenceService service = CamelPreferenceService.getService();
        if (data == null) {
            data = new ArrayList<>();
        }
        service.setXmlPropertyPlaceholders(new ArrayList<>(data));
        setModified(false);
    }

    @Override
    public void reset() {
        data = new ArrayList<>();
        List<PropertyPlaceholderSettingsEntry> entries = CamelPreferenceService.getService().getXmlPropertyPlaceholders();
        if (entries != null) {
            data.addAll(entries);
        }
        if (tablePanel != null) {
            tablePanel.setData(data);
        }
        setModified(false);
    }

    @Override
    public void disposeUIResources() {
        root = null;
        tablePanel = null;
        data = new ArrayList<>();
    }

    private enum Column {
        
        START_TOKEN("Start Token", String.class, PropertyPlaceholderSettingsEntry::getStartToken, null),
        END_TOKEN("End Token", String.class, PropertyPlaceholderSettingsEntry::getEndToken, null),
        NAMESPACES("Namespaces", String.class, e -> String.join(", ", e.getNamespaces()), null),
        ENABLED("Enabled", Boolean.class, PropertyPlaceholderSettingsEntry::isEnabled, (e, v) -> e.setEnabled((Boolean) v));

        Column(String displayName, Class<?> columnClass, 
               Function<PropertyPlaceholderSettingsEntry, Object> valueGetter, 
               BiConsumer<PropertyPlaceholderSettingsEntry, Object> valueSetter) {
            this.displayName = displayName;
            this.columnClass = columnClass;
            this.valueGetter = valueGetter;
            this.valueSetter = valueSetter;
        }

        private final String displayName;
        private final Class<?> columnClass;
        private final Function<PropertyPlaceholderSettingsEntry, Object> valueGetter;
        private final BiConsumer<PropertyPlaceholderSettingsEntry, Object> valueSetter;

        public String getDisplayName() {
            return displayName;
        }

        public Class<?> getColumnClass() {
            return columnClass;
        }

        public Function<PropertyPlaceholderSettingsEntry, Object> getValueGetter() {
            return valueGetter;
        }

        public BiConsumer<PropertyPlaceholderSettingsEntry, Object> getValueSetter() {
            return valueSetter;
        }
        
    }
    
    private class PlaceholderTableModel extends AddEditRemovePanel.TableModel<PropertyPlaceholderSettingsEntry> {

        @Override
        public int getColumnCount() {
            return Column.values().length;
        }

        @Override
        public Object getField(PropertyPlaceholderSettingsEntry o, int columnIndex) {
            Column col = Column.values()[columnIndex];
            return col.getValueGetter().apply(o);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            Column col = Column.values()[columnIndex];
            return col.getColumnClass();
        }

        @Override
        public boolean isEditable(int column) {
            Column col = Column.values()[column];
            return col.getValueSetter() != null;
        }

        @Override
        public void setValue(Object aValue, PropertyPlaceholderSettingsEntry data, int columnIndex) {
            Column col = Column.values()[columnIndex];
            BiConsumer<PropertyPlaceholderSettingsEntry, Object> setter = col.getValueSetter();
            if (setter != null) {
                setter.accept(data, aValue);
                PropertyPlaceholdersSettingsPage.this.setModified(true);
            }
        }

        @Override
        public String getColumnName(int column) {
            Column col = Column.values()[column];
            return col.getDisplayName();
        }
    }
}
