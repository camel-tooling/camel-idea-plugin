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


import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.externalSystem.service.execution.cmd.ParametersListLexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.TextFieldCompletionProvider;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenPomFileChooserDescriptor;
import org.jetbrains.idea.maven.execution.MavenRCSettingsWatcher;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.MavenSelectProjectPopup;
import org.jetbrains.idea.maven.execution.MavenSettingsObservable;
import org.jetbrains.idea.maven.execution.RunnerBundle;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CamelSpringBootRunnerConfPanel implements PanelWithAnchor, MavenSettingsObservable {

    protected LabeledComponent<TextFieldWithBrowseButton> workingDirComponent;

    private JCheckBox skipTestsCheckBox;
    private JPanel panel;
    private LabeledComponent<EditorTextField> profilesComponent;
    private JBLabel myFakeLabel;
    private JCheckBox myResolveToWorkspaceCheckBox;
    private FixedSizeButton showProjectTreeButton;
    private JComponent anchor;

    public CamelSpringBootRunnerConfPanel(@NotNull Project project) {

        workingDirComponent.getComponent().addBrowseFolderListener(
                RunnerBundle.message("maven.select.working.directory"), "", project,
                new MavenPomFileChooserDescriptor(project));

        if (!project.isDefault()) {
            TextFieldCompletionProvider profilesCompletionProvider = new TextFieldCompletionProvider(true) {
                @Override
                protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix, @NotNull CompletionResultSet result) {
                    MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
                    for (String profile : manager.getAvailableProfiles()) {
                        result.addElement(LookupElementBuilder.create(ParametersListUtil.join(profile)));
                    }
                }

                @NotNull
                @Override
                protected String getPrefix(@NotNull String currentTextPrefix) {
                    ParametersListLexer lexer = new ParametersListLexer(currentTextPrefix);
                    while (lexer.nextToken()) {
                        if (lexer.getTokenEnd() == currentTextPrefix.length()) {
                            String prefix = lexer.getCurrentToken();
                            if (prefix.startsWith("-") || prefix.startsWith("!")) {
                                prefix = prefix.substring(1);
                            }
                            return prefix;
                        }
                    }

                    return "";
                }
            };

            profilesComponent.setComponent(profilesCompletionProvider.createEditor(project));
        }

        showProjectTreeButton.setIcon(AllIcons.Nodes.Module);

        MavenSelectProjectPopup.attachToWorkingDirectoryField(MavenProjectsManager.getInstance(project),
                workingDirComponent.getComponent().getTextField(),
                showProjectTreeButton,
                workingDirComponent.getComponent());

        setAnchor(profilesComponent.getLabel());
    }

    protected void setData(final MavenRunnerParameters data) {
        String goals = "clean package "
                + (skipTestsCheckBox.isSelected() ? "-DskipTests " : "")
                + "-Dspring-boot.run.fork=false spring-boot:run "
                + "-Dspring-boot.run.arguments=--camel.springboot.debugging=true"; //TODO Should it be camel.main.debugging ?

        data.setWorkingDirPath(workingDirComponent.getComponent().getText());

        List<String> commandLine = ParametersListUtil.parse(goals);
        data.setGoals(commandLine);

        Map<String, Boolean> profilesMap = new LinkedHashMap<>();

        List<String> profiles = ParametersListUtil.parse(profilesComponent.getComponent().getText());

        for (String profile : profiles) {
            boolean isEnabled = true;
            if (profile.startsWith("-") || profile.startsWith("!")) {
                profile = profile.substring(1);
                if (profile.isEmpty()) {
                    continue;
                }

                isEnabled = false;
            }

            profilesMap.put(profile, isEnabled);
        }
        data.setProfilesMap(profilesMap);
    }

    protected void getData(final MavenRunnerParameters data) {
        workingDirComponent.getComponent().setText(data.getWorkingDirPath());
//        myResolveToWorkspaceCheckBox.setSelected(data.isResolveToWorkspace());

        ParametersList parametersList = new ParametersList();

        for (Map.Entry<String, Boolean> entry : data.getProfilesMap().entrySet()) {
            String profileName = entry.getKey();

            if (!entry.getValue()) {
                profileName = '-' + profileName;
            }

            parametersList.add(profileName);
        }

        profilesComponent.getComponent().setText(parametersList.getParametersString());
        skipTestsCheckBox.setSelected(data.getGoals().contains("-DskipTests"));
    }

    public JComponent createComponent() {
        return panel;
    }

    public String getDisplayName() {
        return RunnerBundle.message("maven.runner.parameters.title");
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
        workingDirComponent.setAnchor(anchor);
        profilesComponent.setAnchor(anchor);
    }

    @Override
    public void registerSettingsWatcher(@NotNull MavenRCSettingsWatcher watcher) {
        watcher.registerComponent("workingDir", workingDirComponent);
        watcher.registerComponent("skipTests", skipTestsCheckBox);
        watcher.registerComponent("profiles", profilesComponent);
//        watcher.registerComponent("resolveToWorkspace", myResolveToWorkspaceCheckBox);
    }
}
