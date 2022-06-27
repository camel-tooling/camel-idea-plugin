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

public class CamelExpressionParameters {

    private ComboBox<String> resultTypeCombo;
    private JPanel myMainPanel;
    private ComboBox<String> bodyMediaTypeCombo;
    private JPanel bodyMediaTypePanel;
    private ComboBox<String> outputMediaTypeCombo;
    private JPanel outputMediaTypePanel;

    public ComboBox<String> getResultTypeCombo() {
        return resultTypeCombo;
    }

    public ComboBox<String> getBodyMediaTypeCombo() {
        return bodyMediaTypeCombo;
    }

    public ComboBox<String> getOutputMediaTypeCombo() {
        return outputMediaTypeCombo;
    }

    public JPanel getOutputMediaTypePanel() {
        return outputMediaTypePanel;
    }

    public JPanel getMainPanel() {
        return myMainPanel;
    }

    public JPanel getBodyMediaTypePanel() {
        return bodyMediaTypePanel;
    }

    private void createUIComponents() {
        resultTypeCombo = new ComboBox<>(new String[]{"java.lang.String", "java.lang.Boolean"});
        bodyMediaTypeCombo = new ComboBox<>(new String[]{"application/json", "application/xml", "application/csv", "application/x-java-object", "text/plain"});
        outputMediaTypeCombo = new ComboBox<>(new String[]{"application/json", "application/xml", "application/csv", "application/x-java-object", "text/plain"});
    }

}
