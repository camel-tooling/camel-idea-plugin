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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.github.cameltooling.idea.runner.CamelJBangRunConfiguration;
import com.github.cameltooling.idea.runner.CamelJBangRunConfigurationOptions;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

public class CamelJBangRunnerConfPanel implements PanelWithAnchor {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("java", "groovy", "kts", "js", "jsh", "xml", "yaml");
    protected JPanel panel;
    protected LabeledComponent<TextFieldWithBrowseButton> filesComponent;
    protected LabeledComponent<EditorTextField> commandOptionsComponent;
    protected LabeledComponent<EditorTextField> dependenciesComponent;
    protected JComponent anchor;

    public CamelJBangRunnerConfPanel(@NotNull Project project) {
        filesComponent.getComponent().addActionListener(e -> openFilesChooser(project));
        this.anchor = UIUtil.mergeComponentsWithAnchor(filesComponent, commandOptionsComponent, dependenciesComponent);
    }

    private void openFilesChooser(@NotNull Project project) {
        VirtualFile root = project.getBaseDir();
        FileChooserDescriptor desc = createFileChooserDescriptor(root);
        FileChooser.chooseFiles(
            desc, project, getInitialFileToSelect(root, desc),
            files -> updateFilesComponentContent(root, files)
        );
    }

    @NotNull
    private static VirtualFile getInitialFileToSelect(VirtualFile root, FileChooserDescriptor desc) {
        for (VirtualFile file : root.getChildren()) {
            if (desc.isFileSelectable(file)) {
                return file;
            }
        }
        return root;
    }

    private static FileChooserDescriptor createFileChooserDescriptor(VirtualFile root) {
        FileChooserDescriptor desc = new FileChooserDescriptor(true, false, false, false, false, true)
            .withDescription("The list of Camel JBang files to Run/Debug.")
            .withTitle("Select Files to Run/Debug")
            .withShowFileSystemRoots(false)
            .withShowHiddenFiles(false)
            .withHideIgnored(true)
            .withRoots(root)
            .withTreeRootVisible(false)
            .withFileFilter(file -> SUPPORTED_EXTENSIONS.contains(FileUtilRt.getExtension(file.getName())));
        desc.setForcedToUseIdeaFileChooser(true);
        return desc;
    }

    private void updateFilesComponentContent(VirtualFile root, List<VirtualFile> files) {
        String contentToAppend = extractContentToAppend(root, files);
        if (contentToAppend.isEmpty()) {
            return;
        }
        TextFieldWithBrowseButton component = filesComponent.getComponent();
        String existingContent = component.getText();
        if (existingContent.isBlank()) {
            component.setText(contentToAppend);
        } else {
            component.setText(String.format("%s %s", existingContent, contentToAppend));
        }
    }

    @NotNull
    private static String extractContentToAppend(VirtualFile root, List<VirtualFile> files) {
        StringBuilder contentToAppend = new StringBuilder();
        for (VirtualFile file : files) {
            String path = file.getPath();
            if (root != null && root.getCanonicalPath() != null && path.startsWith(root.getCanonicalPath())) {
                path = path.substring(root.getCanonicalPath().length() + 1);
            }
            if (!contentToAppend.isEmpty()) {
                contentToAppend.append(' ');
            }
            contentToAppend.append(path);
        }
        return contentToAppend.toString();
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
        filesComponent.setAnchor(anchor);
        dependenciesComponent.setAnchor(anchor);
        commandOptionsComponent.setAnchor(anchor);
    }

    public JComponent createComponent() {
        return panel;
    }

    public void fromConfiguration(CamelJBangRunConfiguration configuration) {
        CamelJBangRunConfigurationOptions options = configuration.getOptions();
        List<String> cmdOptions = options.getCmdOptions();
        if (!cmdOptions.isEmpty()) {
            commandOptionsComponent.getComponent().setText(String.join(" ", cmdOptions));
        }
        List<String> files = options.getFiles();
        if (!files.isEmpty()) {
            filesComponent.getComponent().setText(String.join(" ", files));
        }
        List<String> dependencies = options.getDependencies();
        if (!dependencies.isEmpty()) {
            dependenciesComponent.getComponent().setText(String.join(" ", dependencies));
        }
    }

    public void toConfiguration(CamelJBangRunConfiguration configuration) {
        CamelJBangRunConfigurationOptions options = configuration.getOptions();
        String cmdOptions = commandOptionsComponent.getComponent().getText();
        if (cmdOptions.isBlank()) {
            options.setCmdOptions(new ArrayList<>());
        } else {
            options.setCmdOptions(Arrays.asList(cmdOptions.trim().split(" ")));
        }
        String files = filesComponent.getComponent().getText();
        if (files.isBlank()) {
            options.setFiles(new ArrayList<>());
        } else {
            options.setFiles(Arrays.asList(files.trim().split(" ")));
        }
        String dependencies = dependenciesComponent.getComponent().getText();
        if (dependencies.isBlank()) {
            options.setDependencies(new ArrayList<>());
        } else {
            options.setDependencies(Arrays.asList(dependencies.trim().split(" ")));
        }
    }
}
