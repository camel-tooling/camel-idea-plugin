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

import javax.swing.JComponent;

import com.github.cameltooling.idea.runner.CamelRemoteRunConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

public class CamelRemoteSettingsEditor extends SettingsEditor<CamelRemoteRunConfiguration> {

    private final CamelRemoteRunnerConfPanel configurationPanel;

    public CamelRemoteSettingsEditor(CamelRemoteRunConfiguration configuration) {
        this.configurationPanel = new CamelRemoteRunnerConfPanel();
        super.resetFrom(configuration);
    }

    @Override
    protected void resetEditorFrom(@NotNull CamelRemoteRunConfiguration configuration) {
        configurationPanel.fromConfiguration(configuration);
    }

    @Override
    protected void applyEditorTo(@NotNull CamelRemoteRunConfiguration configuration) throws ConfigurationException {
        configurationPanel.toConfiguration(configuration);
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return this.configurationPanel.createComponent();
    }
}
