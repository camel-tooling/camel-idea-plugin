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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NotNull;


/**
 * Table view for displaying list of ignored properties.
 */
public abstract class CamelIgnorePropertyTable extends JBTable {
    private static final int NAME_COLUMN = 0;
    private final CamelIgnorePropertyModel originalManager;

    public CamelIgnorePropertyTable(@NotNull final CamelIgnorePropertyModel model) {
        super(new ModelAdapter(copy(model)));
        originalManager = model;
        setStriped(true);

        setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);

        final TableColumnModel columnModel = getColumnModel();
        final TableColumn nameColumn = columnModel.getColumn(NAME_COLUMN);
        nameColumn.setCellRenderer(new PropertyValueRenderer());
    }

    /**
     * Makes clone of the current model the table editor is modifying
     */
    private static CamelIgnorePropertyModel copy(@NotNull final CamelIgnorePropertyModel model) {
        try {
            return model.clone();
        } catch (CloneNotSupportedException e) {
            assert false : "Should not happen!";
        }
        return null;
    }

    public boolean isModified() {
        final List<String> current = getModel().getIgnoreProperties();

        if (originalManager.size() != current.size()) {
            return true;
        }

        for (int i = 0; i < current.size(); i++) {
            if (!originalManager.get(i).equals(current.get(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @return a list of modified properties
     */
    public List<String> getIgnoredProperties() {
        return getModel().getIgnoreProperties();
    }

    /**
     * reset the property ignore list
     */
    public void reset() {
        getModel().setIgnoreProperties(copy(originalManager));
    }

    protected abstract void apply(@NotNull java.util.List<CamelIgnorePropertyModel> configurations);

    @Override
    public ModelAdapter getModel() {
        return (ModelAdapter) super.getModel();
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (e == null
                || (e instanceof MouseEvent && ((MouseEvent) e).getClickCount() == 1)
                || (e instanceof KeyEvent && ((KeyEvent) e).getKeyCode() == KeyEvent.VK_DOWN)
                || (e instanceof KeyEvent && ((KeyEvent) e).getKeyCode() == KeyEvent.VK_UP)) {
            return false;
        }
        final Object at = getModel().getValueAt(row, column);
        if (!(at instanceof String)) {
            return false;
        }
        String pattern = Messages.showInputDialog("", "Edit property", null, (String) at, null);

        if (pattern != null && !pattern.isEmpty()) {
            getModel().setValueAt(pattern, row, column);
            return true;
        }
        return false;
    }

    private static final class ModelAdapter extends AbstractTableModel implements EditableModel {
        private List<String> myConfigurations;
        private CamelIgnorePropertyModel configurations;

        private ModelAdapter(final CamelIgnorePropertyModel model) {
            myConfigurations = model
                .getPropertyNames()
                .stream()
                .collect(Collectors.toList());
            this.configurations = model;
        }

        @Override
        public String getColumnName(int column) {
            return "Property name";
        }

        @Override
        public int getRowCount() {
            return myConfigurations.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        public List<String> getIgnoreProperties() {
            return myConfigurations;
        }

        public void setIgnoreProperties(CamelIgnorePropertyModel model) {
            configurations = model;
            myConfigurations = configurations
                .getPropertyNames()
                .stream()
                .collect(Collectors.toList());
            fireTableDataChanged();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return 0 <= rowIndex && rowIndex < myConfigurations.size()
                ? myConfigurations.get(rowIndex)
                : null;
        }

        public void add(@NotNull final String property) {
            myConfigurations.add(property);
            configurations.add(property);
            fireTableRowsInserted(myConfigurations.size() - 1, myConfigurations.size() - 1);
        }

        @Override
        public void addRow() {
            String pattern = Messages.showInputDialog("", "Enter pattern", null);

            if (pattern != null && !pattern.isEmpty()) {
                configurations.add(pattern);
                myConfigurations.add(pattern);
                int i = myConfigurations.size() - 1;
                fireTableRowsInserted(i, i);
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            configurations.remove(rowIndex);
            myConfigurations.remove(rowIndex);

            configurations.add(rowIndex, (String) aValue);
            myConfigurations.add(rowIndex, (String) aValue);
            super.setValueAt(aValue, rowIndex, columnIndex);
        }

        @Override
        public void removeRow(int index) {
            configurations.remove(index);
            myConfigurations.remove(index);
            fireTableRowsDeleted(index, index);
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            configurations.add(newIndex, configurations.remove(oldIndex));
            myConfigurations.add(newIndex, myConfigurations.remove(oldIndex));
            fireTableRowsUpdated(Math.min(oldIndex, newIndex), Math.max(oldIndex, newIndex));
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex) {
            return true;
        }
    }

    private static class PropertyValueRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        }

        @Override
        protected void setValue(Object value) {
            Icon icon = null;
            String text = null;
            if (value instanceof String) {
                icon = getIcon((String) value);
                text = getText((String) value);
            }
            setIcon(icon);
            setText(text == null ? "" : text);
        }

        Icon getIcon(String propertyName) {
            return null;
        }

        String getText(String propertyName) {
            return propertyName;
        }
    }
}