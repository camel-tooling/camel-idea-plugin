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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class PropertyPlaceholderEditDialog extends DialogWrapper {

    private JTextField startField;
    private JTextField endField;

    private DefaultListModel<String> namespacesModel;
    private JBList<String> namespacesList;

    PropertyPlaceholderEditDialog(Project project) {
        super(project, true);
        init();
    }

    @Override
    protected void init() {
        setTitle("Edit Property Placeholder Definition");
        startField = new JTextField();
        endField = new JTextField();
        namespacesModel = new DefaultListModel<>();
        namespacesList = new JBList<>(namespacesModel);
        namespacesList.setVisibleRowCount(6);
        super.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.anchor = GridBagConstraints.WEST;
        gbcLabel.insets = JBUI.insets(5, 5, 0, 5);

        GridBagConstraints gbcField = new GridBagConstraints();
        gbcField.gridx = 1;
        gbcField.weightx = 1.0;
        gbcField.fill = GridBagConstraints.HORIZONTAL;
        gbcField.insets = JBUI.insets(5, 0, 0, 5);

        int row = 0;
        gbcLabel.gridy = row;
        gbcField.gridy = row;
        panel.add(new JBLabel("Start token:"), gbcLabel);
        panel.add(startField, gbcField);

        row++;
        gbcLabel.gridy = row;
        gbcField.gridy = row;
        panel.add(new JBLabel("End token:"), gbcLabel);
        panel.add(endField, gbcField);

        row++;
        gbcLabel.gridy = row;
        gbcField.gridy = row;
        gbcField.weighty = 1.0;
        gbcField.fill = GridBagConstraints.BOTH;

        panel.add(new JBLabel("XML namespaces:"), gbcLabel);

        // Decorate the list with add/remove/edit actions
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(namespacesList)
                .setAddAction(button -> addNamespace())
                .setEditAction(button -> editSelectedNamespace())
                .setRemoveAction(button -> removeSelectedNamespaces());
        JComponent listPanel = decorator.createPanel();
        panel.add(listPanel, gbcField);

        startField.setPreferredSize(new Dimension(200, startField.getPreferredSize().height));
        endField.setPreferredSize(new Dimension(200, endField.getPreferredSize().height));

        return panel;
    }

    private void addNamespace() {
        String value = JOptionPane.showInputDialog(getContentPanel(), "New namespace:", "Add Namespace", JOptionPane.PLAIN_MESSAGE);
        if (value != null) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty() && !containsNamespace(trimmed)) {
                namespacesModel.addElement(trimmed);
            }
        }
    }

    private void editSelectedNamespace() {
        int idx = namespacesList.getSelectedIndex();
        if (idx >= 0) {
            String current = namespacesModel.get(idx);
            String value = JOptionPane.showInputDialog(getContentPanel(), "Edit namespace:", current);
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty() && (!containsNamespace(trimmed) || trimmed.equals(current))) {
                    namespacesModel.set(idx, trimmed);
                }
            }
        }
    }

    private void removeSelectedNamespaces() {
        int[] indices = namespacesList.getSelectedIndices();
        if (indices.length > 0) {
            // Remove from last to first to keep indices valid
            for (int i = indices.length - 1; i >= 0; i--) {
                namespacesModel.remove(indices[i]);
            }
        }
    }

    private boolean containsNamespace(String ns) {
        for (int i = 0; i < namespacesModel.getSize(); i++) {
            if (namespacesModel.get(i).equals(ns)) return true;
        }
        return false;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return startField;
    }

    void initValues(PropertyPlaceholderSettingsEntry entry) {
        if (entry != null) {
            startField.setText(entry.getStartToken());
            endField.setText(entry.getEndToken());
            namespacesModel.clear();
            for (String ns : entry.getNamespaces()) {
                if (ns != null) {
                    String trimmed = ns.trim();
                    if (!trimmed.isEmpty() && !containsNamespace(trimmed)) {
                        namespacesModel.addElement(trimmed);
                    }
                }
            }
        }
    }

    String getStartToken() {
        return startField.getText().trim();
    }

    String getEndToken() {
        return endField.getText().trim();
    }

    List<String> getNamespaces() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < namespacesModel.getSize(); i++) {
            result.add(namespacesModel.get(i));
        }
        return result;
    }

}
