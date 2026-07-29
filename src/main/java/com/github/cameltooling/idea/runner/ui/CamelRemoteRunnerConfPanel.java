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

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;

import com.github.cameltooling.idea.runner.CamelRemoteRunConfiguration;
import com.github.cameltooling.idea.runner.CamelRemoteRunConfigurationOptions;
import com.github.cameltooling.idea.runner.debugger.JmxProtocol;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.util.BooleanFunction;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelRemoteRunnerConfPanel implements PanelWithAnchor {

    protected JPanel panel;
    protected JComponent anchor;
    protected LabeledComponent<JBTextField> hostComponent;
    protected LabeledComponent<JBTextField> portComponent;
    protected JRadioButton rmiRadioButton;
    protected JRadioButton jolokiaRadioButton;
    protected JRadioButton customRadioButton;
    protected JCheckBox sslCheckBox;
    protected LabeledComponent<JBTextField> serviceUrlComponent;

    private JmxProtocol selectedProtocol = JmxProtocol.getDefault();
    private boolean lastSslState = false;
    private String lastCustomUrl = "";

    public CamelRemoteRunnerConfPanel() {
        this.anchor = UIUtil.mergeComponentsWithAnchor(hostComponent, portComponent);

        rmiRadioButton.addActionListener(e -> onProtocolModeChanged(JmxProtocol.RMI));
        jolokiaRadioButton.addActionListener(e -> onProtocolModeChanged(JmxProtocol.JOLOKIA));
        customRadioButton.addActionListener(e -> onProtocolModeChanged(JmxProtocol.CUSTOM));

        sslCheckBox.addActionListener(e -> {
            lastSslState = sslCheckBox.isSelected();
            updateModeState();
        });

        DocumentAdapter hostPortListener = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent event) {
                updateServiceUrlFromFields();
            }
        };
        hostComponent.getComponent().getDocument().addDocumentListener(hostPortListener);
        portComponent.getComponent().getDocument().addDocumentListener(hostPortListener);

        BooleanFunction<JBTextField> showEmptyTextWhenFocused = tf -> tf.getText().isEmpty();
        hostComponent.getComponent().putClientProperty(TextComponentEmptyText.STATUS_VISIBLE_FUNCTION, showEmptyTextWhenFocused);
        portComponent.getComponent().putClientProperty(TextComponentEmptyText.STATUS_VISIBLE_FUNCTION, showEmptyTextWhenFocused);
    }

    private void onProtocolModeChanged(@Nullable JmxProtocol jmxProtocol) {
        if (selectedProtocol == JmxProtocol.CUSTOM && jmxProtocol != JmxProtocol.CUSTOM) {
            lastCustomUrl = serviceUrlComponent.getComponent().getText();
        }
        boolean switchingToCustom = selectedProtocol != JmxProtocol.CUSTOM && jmxProtocol == JmxProtocol.CUSTOM;
        selectedProtocol = jmxProtocol;
        if (switchingToCustom && !lastCustomUrl.isEmpty()) {
            serviceUrlComponent.getComponent().setText(lastCustomUrl);
        }
        updateModeState();
    }

    private void updateModeState() {
        boolean isCustomProtocol = selectedProtocol == JmxProtocol.CUSTOM;
        hostComponent.setVisible(!isCustomProtocol);
        portComponent.setVisible(!isCustomProtocol);
        // SSL is only meaningful for Jolokia: RMI encodes TLS differently and Custom carries it in the raw URL.
        boolean isJolokia = selectedProtocol == JmxProtocol.JOLOKIA;
        sslCheckBox.setVisible(isJolokia);
        if (!isJolokia) {
            sslCheckBox.setSelected(false);
        } else {
            sslCheckBox.setSelected(lastSslState);
        }

        JBTextField serviceUrlField = serviceUrlComponent.getComponent();
        serviceUrlField.setEditable(isCustomProtocol);
        // Stays selectable/copyable (setEditable, not setEnabled); dim the text to signal the computed, read-only
        // value. The background is not toggled because the text-field LAF paints its own and ignores setBackground().
        serviceUrlField.setForeground(isCustomProtocol ? UIUtil.getTextFieldForeground() : UIUtil.getInactiveTextColor());

        if (!isCustomProtocol) {
            hostComponent.getComponent().getEmptyText().setText("localhost");
            portComponent.getComponent().getEmptyText().setText(String.valueOf(selectedProtocol.getDefaultPort(sslCheckBox.isSelected())));
        }

        updateServiceUrlFromFields();
    }

    private void updateServiceUrlFromFields() {
        if (selectedProtocol == JmxProtocol.CUSTOM) {
            return;
        }
        String hostText = hostComponent.getComponent().getText().trim();
        String host = hostText.isEmpty() ? "localhost" : hostText;
        String portText = portComponent.getComponent().getText().trim();
        int port;
        try {
            port = portText.isEmpty() ? selectedProtocol.getDefaultPort(sslCheckBox.isSelected()) : Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            port = 0;
        }
        Boolean useSsl = selectedProtocol == JmxProtocol.JOLOKIA ? sslCheckBox.isSelected() : null;
        String serviceUrl = JmxProtocol.buildServiceUrl(selectedProtocol, host, port, useSsl);
        serviceUrlComponent.getComponent().setText(serviceUrl);
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
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
        int port = options.getPort();
        portComponent.getComponent().setText(port <= 0 ? "" : Integer.toString(port));
        selectedProtocol = options.getProtocol();
        switch (selectedProtocol) {
            case RMI     -> rmiRadioButton.setSelected(true);
            case JOLOKIA -> jolokiaRadioButton.setSelected(true);
            case CUSTOM  -> customRadioButton.setSelected(true);
        }
        sslCheckBox.setSelected(options.isSsl());
        lastSslState = options.isSsl();
        if (options.getProtocol() == JmxProtocol.CUSTOM) {
            lastCustomUrl = options.getServiceUrl();
        }
        serviceUrlComponent.getComponent().setText(options.getEffectiveServiceUrl());
        updateModeState();
    }

    public void toConfiguration(CamelRemoteRunConfiguration configuration) throws ConfigurationException {
        CamelRemoteRunConfigurationOptions options = configuration.getOptions();

        String host = hostComponent.getComponent().getText().trim();
        options.setHost(host);

        String port = portComponent.getComponent().getText().trim();
        int value;
        if (port.isEmpty()) {
            value = 0;
        } else {
            try {
                value = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                throw new ConfigurationException("The port must be a number between 1 and 65535");
            }
            if (value < 1 || value > 65535) {
                throw new ConfigurationException("The port must be between 1 and 65535");
            }
        }
        options.setPort(value);

        options.setProtocol(selectedProtocol);
        options.setSsl(sslCheckBox.isSelected());

        if (selectedProtocol == JmxProtocol.CUSTOM) {
            String url = serviceUrlComponent.getComponent().getText().trim();
            if (url.isEmpty()) {
                throw new ConfigurationException("The JMX Service URL cannot be empty");
            }
            options.setServiceUrl(url);
        } else {
            options.setServiceUrl("");
        }
    }
}
