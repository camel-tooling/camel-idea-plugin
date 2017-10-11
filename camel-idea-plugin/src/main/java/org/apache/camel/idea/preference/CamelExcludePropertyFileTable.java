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
import java.awt.event.MouseEvent;
import java.util.EventObject;
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
 * Table view for displaying list of excluded yml/properties files.
 */
public abstract class CamelExcludePropertyFileTable extends JBTable {
    private static final int NAME_COLUMN = 0;
    private final CamelExcludePropertyFileModel originalManager;

    public CamelExcludePropertyFileTable(@NotNull final CamelExcludePropertyFileModel model) {
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
     * @param model
     */
    private static CamelExcludePropertyFileModel copy(@NotNull final CamelExcludePropertyFileModel model) {
        try {
            return model.clone();
        } catch (CloneNotSupportedException e) {
            assert false : "Should not happen!";
        }
        return null;
    }

    public boolean isModified() {
        final List<String> current = getModel().getExcludePropertyFiles();

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
     * @return a list of modified filenames
     */
    public List<String> getExcludePropertyFiles() {
        return getModel().getExcludePropertyFiles();
    }

    /**
     * reset the filename exclude list
     */
    public void reset() {
        getModel().setExcludPropertyFiles(copy(originalManager));
    }

    protected abstract void apply(@NotNull List<CamelExcludePropertyFileModel> excludePropertyFilesModel);

    @Override
    public ModelAdapter getModel() {
        return (ModelAdapter) super.getModel();
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (e == null || (e instanceof MouseEvent && ((MouseEvent) e).getClickCount() == 1)) {
            return false;
        }
        final Object at = getModel().getValueAt(row, column);
        if (!(at instanceof String)) {
            return false;
        }
        String pattern = Messages.showInputDialog("", "Edit filename pattern", null, (String) at, null);

        if (pattern != null && !pattern.isEmpty()) {
            getModel().setValueAt(pattern, row, column);
            return true;
        }
        return false;
    }

    static final class ModelAdapter extends AbstractTableModel implements EditableModel {
        private List<String> excludedPropertyFiles;
        private CamelExcludePropertyFileModel excludePropertyFilesModel;

        private ModelAdapter(final CamelExcludePropertyFileModel model) {
            excludedPropertyFiles = model
                .getFilenames()
                .stream()
                .collect(Collectors.toList());
            this.excludePropertyFilesModel = model;
        }

        @Override
        public String getColumnName(int column) {
            return "Filename";
        }

        @Override
        public int getRowCount() {
            return excludedPropertyFiles.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        public List<String> getExcludePropertyFiles() {
            return excludedPropertyFiles;
        }

        public void setExcludPropertyFiles(CamelExcludePropertyFileModel model) {
            excludePropertyFilesModel = model;
            excludedPropertyFiles = excludePropertyFilesModel
                .getFilenames()
                .stream()
                .collect(Collectors.toList());
            fireTableDataChanged();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return 0 <= rowIndex && rowIndex < excludedPropertyFiles.size()
                ? excludedPropertyFiles.get(rowIndex)
                : null;
        }

        public void add(@NotNull final String property) {
            excludedPropertyFiles.add(property);
            excludePropertyFilesModel.add(property);
            fireTableRowsInserted(excludedPropertyFiles.size() - 1, excludedPropertyFiles.size() - 1);
        }

        @Override
        public void addRow() {
            String pattern = Messages.showInputDialog("", "Enter filename pattern (**/file.properties)", null);

            if (pattern != null && !pattern.isEmpty()) {
                excludePropertyFilesModel.add(pattern);
                excludedPropertyFiles.add(pattern);
                int i = excludedPropertyFiles.size() - 1;
                fireTableRowsInserted(i, i);
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            excludePropertyFilesModel.remove(rowIndex);
            excludedPropertyFiles.remove(rowIndex);

            excludePropertyFilesModel.add(rowIndex, (String) aValue);
            excludedPropertyFiles.add(rowIndex, (String) aValue);
            super.setValueAt(aValue, rowIndex, columnIndex);
        }

        @Override
        public void removeRow(int index) {
            excludePropertyFilesModel.remove(index);
            excludedPropertyFiles.remove(index);
            fireTableRowsDeleted(index, index);
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            excludePropertyFilesModel.add(newIndex, excludePropertyFilesModel.remove(oldIndex));
            excludedPropertyFiles.add(newIndex, excludedPropertyFiles.remove(oldIndex));
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

        Icon getIcon(String filename) {
            return null;
        }

        String getText(String filename) {
            return filename;
        }
    }
}