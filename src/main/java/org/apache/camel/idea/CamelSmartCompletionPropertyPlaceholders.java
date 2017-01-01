/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

/**
 * Smart completion for editing a Camel endpoint uri, to show a list of property holders can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class CamelSmartCompletionPropertyPlaceholders {

    //TODO Allow this to be configurable
    private static final String[] IGNORE_PROPERTIES = new String[]{"java.", "Logger.", "logger", "appender.", "rootLogger.",
            // ignore camel-spring-boot auto configuration prefixes
            "camel.springboot.", "camel.component.", "camel.dataformat.", "camel.language."};

    public void propertyPlaceholdersSmartCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        Project project = parameters.getOriginalFile().getManager().getProject();

        List<VirtualFile> resourceRoots = ProjectRootManager.getInstance(project).getModuleSourceRoots(JavaModuleSourceRootTypes.PRODUCTION);
        resourceRoots.addAll(ProjectRootManager.getInstance(project).getModuleSourceRoots(JavaModuleSourceRootTypes.TESTS));
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (final VirtualFile sourceRoot : resourceRoots) {
            VirtualFile file = sourceRoot.getCanonicalFile();
            if (file != null) {
                VfsUtil.processFilesRecursively(file, virtualFile -> {
                    if (virtualFile.isValid() && virtualFile.getName().endsWith(".properties") && !projectFileIndex.isExcluded(sourceRoot)) {
                        loadProperties(virtualFile).forEach((key, value) -> buildResultSet(resultSet, (String) key, (String) value));
                    }
                    return true;
                });
            }
        }
    }

    @NotNull
    private Properties loadProperties(VirtualFile virtualFile) {
        Properties properties = new Properties();

        try {
            File file = new File(virtualFile.getPath());
            properties.load(Files.newInputStream(file.toPath()));
        } catch (IOException e) {
            // ignore
        }

        return properties;
    }

    private void buildResultSet(@NotNull CompletionResultSet resultSet, String key, String value) {
        for (String ignore : IGNORE_PROPERTIES) {
            if (key.startsWith(ignore)) {
                return;
            }
        }
        LookupElementBuilder builder = LookupElementBuilder.create(key + "}}")
                .appendTailText(value, false)
                .withPresentableText(key + " = ");
        resultSet.withPrefixMatcher(new PlainPrefixMatcher("")).addElement(builder);
    }

}
