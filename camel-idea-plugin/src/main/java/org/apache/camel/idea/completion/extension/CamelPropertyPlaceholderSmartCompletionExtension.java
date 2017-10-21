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
package org.apache.camel.idea.completion.extension;

import java.util.ArrayList;
import java.util.List;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

/**
 * Camel property placeholder smart completion.
 * <p/>
 * Such as a Camel endpoint uri, to show a list of properties can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties. However it works for any Camel property placeholder
 * used in your source code.
 */
public class CamelPropertyPlaceholderSmartCompletionExtension implements CamelCompletionExtension {

    private final List<CamelPropertyCompletion> propertyCompletionProviders = new ArrayList<>();

    public CamelPropertyPlaceholderSmartCompletionExtension() {
        propertyCompletionProviders.add(new PropertiesPropertyPlaceholdersSmartCompletion());
        propertyCompletionProviders.add(new YamlPropertyPlaceholdersSmartCompletion());
    }

    @Override
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull String[] query) {
        Project project = parameters.getOriginalFile().getManager().getProject();

        List<VirtualFile> resourceRoots = ProjectRootManager.getInstance(project).getModuleSourceRoots(JavaModuleSourceRootTypes.PRODUCTION);
        resourceRoots.addAll(ProjectRootManager.getInstance(project).getModuleSourceRoots(JavaModuleSourceRootTypes.TESTS));
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (final VirtualFile sourceRoot : resourceRoots) {
            if (sourceRoot.isValid() && sourceRoot.getCanonicalFile() != null) {
                VfsUtil.processFilesRecursively(sourceRoot.getCanonicalFile(), virtualFile -> {
                    propertyCompletionProviders.stream()
                        .filter(p -> p.isValidExtension(virtualFile.getCanonicalPath()) && !projectFileIndex.isExcluded(sourceRoot))
                        .forEach(p -> p.buildResultSet(resultSet, virtualFile));
                    return true;
                });
            }
        }
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, ProcessingContext context, String[] query) {
        if (query[0].endsWith("{{")) {
            return true;
        }
        return false;
    }
}
