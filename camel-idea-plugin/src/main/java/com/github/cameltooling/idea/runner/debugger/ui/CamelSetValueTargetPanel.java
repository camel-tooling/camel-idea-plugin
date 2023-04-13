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
package com.github.cameltooling.idea.runner.debugger.ui;

import com.github.cameltooling.idea.runner.debugger.CamelDebuggerTarget;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CamelSetValueTargetPanel {
    private ComboBox<CamelDebuggerTarget> targetComboBox;
    private JTextField targetName;
    private JPanel myPanel;

    private void createUIComponents() {
        targetComboBox = new ComboBox<>(CamelDebuggerTarget.values());
        targetComboBox.setRenderer(
            new SimpleListCellRenderer<>() {
                @Override
                public void customize(@NotNull JList<? extends CamelDebuggerTarget> list, CamelDebuggerTarget value,
                                      int index, boolean selected, boolean hasFocus) {
                    this.setText(value.getName());
                }
            }
        );
        targetComboBox.addActionListener(e -> targetName.setVisible(targetComboBox.getItem() != CamelDebuggerTarget.BODY));
    }

    public CamelDebuggerTarget getTargetType() {
        return targetComboBox.getItem();
    }

    public String getTargetName() {
        return targetComboBox.getItem() == CamelDebuggerTarget.BODY ? null : targetName.getText();
    }

    public JTextField getTargetNameComponent() {
        return targetName;
    }

    public JPanel getPanel() {
        return myPanel;
    }
}
