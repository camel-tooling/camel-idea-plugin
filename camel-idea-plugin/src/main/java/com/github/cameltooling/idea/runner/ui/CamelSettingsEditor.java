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

import com.github.cameltooling.idea.runner.CamelRunConfiguration;
import com.github.cameltooling.idea.runner.CamelSpringBootRunConfigurationType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CamelSettingsEditor extends SettingsEditor<CamelRunConfiguration> {
    private AbstractCamelRunnerConfPanel configurationPanel;

    public CamelSettingsEditor(CamelRunConfiguration runnerConfiguration) {
        this.configurationPanel = CamelSpringBootRunConfigurationType.ID.equals(runnerConfiguration.getType().getId()) ?
                new CamelSpringBootRunnerConfPanel(runnerConfiguration.getProject()) :
                new CamelRunnerConfPanel(runnerConfiguration.getProject());

        super.resetFrom(runnerConfiguration);
    }

    @Override
    protected void resetEditorFrom(@NotNull CamelRunConfiguration runnerConfiguration) {
        configurationPanel.getData(runnerConfiguration.getRunnerParameters());
    }

    @Override
    protected void applyEditorTo(@NotNull CamelRunConfiguration runnerConfiguration) throws ConfigurationException {
        configurationPanel.setData(runnerConfiguration.getRunnerParameters());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return this.configurationPanel.createComponent();
    }
}
