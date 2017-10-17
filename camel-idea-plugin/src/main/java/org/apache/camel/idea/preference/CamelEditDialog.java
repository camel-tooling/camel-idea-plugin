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
import javax.swing.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.xml.XmlBundle;

public class CamelEditDialog extends DialogWrapper {

    private JTextField myTfUrl;

    private final String myTitle;
    private final String myName;
    private String textFieldText;

    CamelEditDialog(Project project) {
        super(project, true);
        myTitle = XmlBundle.message("dialog.title.external.resource");
        myName = XmlBundle.message("label.edit.external.resource.uri");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        panel.add(
                new JLabel(myName),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 3, 5), 0, 0)
        );
        panel.add(
                myTfUrl,
                new GridBagConstraints(0, 1, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0)
        );

        myTfUrl.setPreferredSize(new Dimension(350, myTfUrl.getPreferredSize().height));
        return panel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myTfUrl;
    }

    @Override
    protected void init() {
        setTitle(myTitle);
        myTfUrl = new JTextField();
        super.init();
    }

    String getTextFieldText() {
        return textFieldText;
    }

    void init(String s) {
        myTfUrl.setText(s);
    }
}