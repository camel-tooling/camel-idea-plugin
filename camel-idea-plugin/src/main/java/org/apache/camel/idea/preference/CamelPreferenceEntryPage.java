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
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preference UI for this plugin.
 */
public class CamelPreferenceEntryPage implements SearchableConfigurable, Configurable.NoScroll {

    CamelPreferenceEntryPage() {
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel result = new JPanel(new BorderLayout());
        result.add(new JBLabel("Configure Apache Camel editor settings."), BorderLayout.PAGE_START);
        
        return result;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Apache Camel";
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.CamelConfigurable";
    }
}
