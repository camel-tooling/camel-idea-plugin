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
package com.github.cameltooling.idea.runner.ui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.github.cameltooling.idea.runner.CamelRemoteRunConfiguration;
import com.github.cameltooling.idea.runner.CamelRemoteRunConfigurationOptions;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

public class CamelRemoteRunnerConfPanel implements PanelWithAnchor {
    protected JPanel panel;
    protected JComponent anchor;
    protected LabeledComponent<EditorTextField> hostComponent;
    protected LabeledComponent<EditorTextField> portComponent;

    public CamelRemoteRunnerConfPanel() {
        this.anchor = UIUtil.mergeComponentsWithAnchor(hostComponent, portComponent);
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
        hostComponent.setAnchor(anchor);
        portComponent.setAnchor(anchor);
    }

    @Override
    public @Nullable JComponent getOwnAnchor() {
        return PanelWithAnchor.super.getOwnAnchor();
    }

    public JComponent createComponent() {
        return panel;
    }


    public void fromConfiguration(CamelRemoteRunConfiguration configuration) {
        CamelRemoteRunConfigurationOptions options = configuration.getOptions();
        hostComponent.getComponent().setText(options.getHost());
        portComponent.getComponent().setText(Integer.toString(options.getPort()));
    }

    public void toConfiguration(CamelRemoteRunConfiguration configuration) throws ConfigurationException {
        CamelRemoteRunConfigurationOptions options = configuration.getOptions();
        String host = hostComponent.getComponent().getText();
        if (host.isBlank()) {
            throw new ConfigurationException("The host name cannot be empty");
        } else {
            options.setHost(host.trim());
        }
        String port = portComponent.getComponent().getText();
        if (port.isBlank()) {
            throw new ConfigurationException("The port cannot be empty");
        } else {
            try {
                Integer value = Integer.valueOf(port.trim());
                if (value <= 0) {
                    throw new ConfigurationException("The port must be positive");
                }
                options.setPort(value);
            } catch (NumberFormatException e) {
                throw new ConfigurationException("The port must be a number");
            }
        }
    }
}
