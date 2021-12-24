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

import com.intellij.openapi.ui.ComboBox;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CamelSetValueTargetPanel {
    private ComboBox<String> targetComboBox;
    private JTextField targetName;
    private JPanel myPanel;

    private void createUIComponents() {
        targetComboBox = new ComboBox(new String[]{"Message Header", "Exchange Property", "Body"});
        targetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                targetName.setVisible(!("Body".equals(targetComboBox.getItem())));
            }
        });
    }

    public String getTargetType() {
        return targetComboBox.getItem();
    }

    public String getTargetName() {
        return "Body".equals(targetComboBox.getItem()) ? null : targetName.getText();
    }

    public JTextField getTargetNameComponent() {
        return targetName;
    }

    public JPanel getPanel() {
        return myPanel;
    }
}
