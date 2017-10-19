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
import javax.swing.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

class CamelEditDialog extends DialogWrapper {

    private JTextField textfield;

    CamelEditDialog(Project project) {
        super(project, true);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        Insets insets = JBUI.insets(0, 5, 5, 5);
        GridBagConstraints gbc = new GridBagConstraints(0, 1, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0);
        panel.add(textfield, gbc);

        textfield.setPreferredSize(new Dimension(350, textfield.getPreferredSize().height));

        return panel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return textfield;
    }

    @Override
    protected void init() {
        setTitle("Enter filename pattern (**/file.properties)");
        textfield = new JTextField();
        super.init();
    }

    String getTextFieldText() {
        return textfield.getText();
    }

    /**
     * Initializes editor with the passed data.
     */
    void init(String origin) {
        textfield.setText(origin);
    }
}