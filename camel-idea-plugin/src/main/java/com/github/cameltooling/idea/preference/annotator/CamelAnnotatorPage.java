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
package com.github.cameltooling.idea.preference.annotator;

import java.awt.*;
import javax.swing.*;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.components.JBCheckBox;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Annotator (real time validation) UI for this plugin.
 */
public class CamelAnnotatorPage implements SearchableConfigurable, Configurable.NoScroll {

    private JBCheckBox realTimeEndpointValidationCatalogCheckBox;
    private JBCheckBox highlightCustomOptionsCheckBox;
    private JBCheckBox realTimeSimpleValidationCatalogCheckBox;
    private JBCheckBox realTimeJSonPathValidationCatalogCheckBox;
    private JBCheckBox realTimeIdReferenceTypeValidationCheckBox;
    private JBCheckBox realTimeBeanMethodValidationCheckBox;
    private JPanel result;
    private JPanel jPanel;

    CamelAnnotatorPage() {
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        realTimeEndpointValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel endpoints in editor");
        highlightCustomOptionsCheckBox = new JBCheckBox("Highlight custom endpoint options as warnings in editor");
        realTimeSimpleValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel simple language in editor");
        realTimeJSonPathValidationCatalogCheckBox = new JBCheckBox("Real time validation of Camel JSonPath language in editor");
        realTimeIdReferenceTypeValidationCheckBox = new JBCheckBox("Real time type validation when referencing beans");
        realTimeBeanMethodValidationCheckBox = new JBCheckBox("Real time validation of call bean method in editor");

        // use mig layout which is like a spread-sheet with 2 columns, which we can span if we only have one element
        jPanel = new JPanel(new MigLayout("fillx,wrap 2", "[left]rel[grow,fill]"));
        jPanel.setOpaque(false);

        jPanel.add(realTimeEndpointValidationCatalogCheckBox, "span 2");
        jPanel.add(highlightCustomOptionsCheckBox, "span 2");
        jPanel.add(realTimeSimpleValidationCatalogCheckBox, "span 2");
        jPanel.add(realTimeJSonPathValidationCatalogCheckBox, "span 2");
        jPanel.add(realTimeIdReferenceTypeValidationCheckBox, "span 2");
        jPanel.add(realTimeBeanMethodValidationCheckBox, "span 2");

        result = new JPanel(new BorderLayout());
        result.add(jPanel, BorderLayout.NORTH);
        reset();
        return result;
    }

    @Override
    public void apply() {
        getCamelPreferenceService().setRealTimeEndpointValidation(realTimeEndpointValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setHighlightCustomOptions(highlightCustomOptionsCheckBox.isSelected());
        getCamelPreferenceService().setRealTimeSimpleValidation(realTimeSimpleValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setRealTimeJSonPathValidation(realTimeJSonPathValidationCatalogCheckBox.isSelected());
        getCamelPreferenceService().setRealTimeIdReferenceTypeValidation(realTimeIdReferenceTypeValidationCheckBox.isSelected());
        getCamelPreferenceService().setRealTimeBeanMethodValidationCheckBox(realTimeBeanMethodValidationCheckBox.isSelected());
    }

    @Override
    public boolean isModified() {
        // check boxes
        boolean b1 = getCamelPreferenceService().isRealTimeEndpointValidation() != realTimeEndpointValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isHighlightCustomOptions() != highlightCustomOptionsCheckBox.isSelected()
            || getCamelPreferenceService().isRealTimeSimpleValidation() != realTimeSimpleValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isRealTimeJSonPathValidation() != realTimeJSonPathValidationCatalogCheckBox.isSelected()
            || getCamelPreferenceService().isRealTimeIdReferenceTypeValidation() != realTimeIdReferenceTypeValidationCheckBox.isSelected()
            || getCamelPreferenceService().isRealTimeBeanMethodValidationCheckBox() != realTimeBeanMethodValidationCheckBox.isSelected();
        return b1;
    }

    @Override
    public void reset() {
        realTimeEndpointValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeEndpointValidation());
        highlightCustomOptionsCheckBox.setSelected(getCamelPreferenceService().isHighlightCustomOptions());
        realTimeSimpleValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeSimpleValidation());
        realTimeJSonPathValidationCatalogCheckBox.setSelected(getCamelPreferenceService().isRealTimeJSonPathValidation());
        realTimeIdReferenceTypeValidationCheckBox.setSelected(getCamelPreferenceService().isRealTimeIdReferenceTypeValidation());
        realTimeBeanMethodValidationCheckBox.setSelected(getCamelPreferenceService().isRealTimeBeanMethodValidationCheckBox());
    }

    @Override
    public void disposeUIResources() {
        realTimeEndpointValidationCatalogCheckBox = null;
        highlightCustomOptionsCheckBox = null;
        realTimeSimpleValidationCatalogCheckBox = null;
        realTimeJSonPathValidationCatalogCheckBox = null;
        realTimeIdReferenceTypeValidationCheckBox = null;
        realTimeBeanMethodValidationCheckBox = null;

        jPanel.removeAll();
        jPanel = null;
        result.removeAll();
        result = null;
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

    CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

    JBCheckBox getRealTimeEndpointValidationCatalogCheckBox() {
        return realTimeEndpointValidationCatalogCheckBox;
    }

    JBCheckBox getHighlightCustomOptionsCheckBox() {
        return highlightCustomOptionsCheckBox;
    }

    JBCheckBox getRealTimeSimpleValidationCatalogCheckBox() {
        return realTimeSimpleValidationCatalogCheckBox;
    }

    JBCheckBox getRealTimeJSonPathValidationCatalogCheckBox() {
        return realTimeJSonPathValidationCatalogCheckBox;
    }

    JBCheckBox getRealTimeIdReferenceTypeValidationCheckBox() {
        return realTimeIdReferenceTypeValidationCheckBox;
    }

    public JBCheckBox getRealTimeBeanMethodValidationCheckBox() {
        return realTimeBeanMethodValidationCheckBox;
    }
}
