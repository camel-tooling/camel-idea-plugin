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
package com.github.cameltooling.idea.completion.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;

/**
 * Camel property placeholder smart completion.
 * <p/>
 * Such as a Camel endpoint uri, to show a list of properties can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties. However it works for any Camel property placeholder
 * used in your source code.
 */
public class CamelPropertyPlaceholderSmartCompletionExtension implements CamelCompletionExtension {

    private static final Logger LOG = Logger.getInstance(CamelPropertyPlaceholderSmartCompletionExtension.class);

    private final List<CamelPropertyCompletion> propertyCompletionProviders = new ArrayList<>();

    public CamelPropertyPlaceholderSmartCompletionExtension() {
        propertyCompletionProviders.add(new PropertiesPropertyPlaceholdersSmartCompletion());
        propertyCompletionProviders.add(new YamlPropertyPlaceholdersSmartCompletion());
    }

    @Override
    //TODO: might be better to reimplement based on IDEA's PropertiesCompletionContributor, via PropertyReferences
    public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull CompletionQuery query) {
        PsiFile originalFile = parameters.getOriginalFile();
        Project project = originalFile.getManager().getProject();

        List<VirtualFile> resourceRoots = getResourceRoots(project, originalFile);
        if (resourceRoots.isEmpty()) {
            return;
        }

        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (final VirtualFile sourceRoot : resourceRoots) {
            if (projectFileIndex.isExcluded(sourceRoot)) {
                continue;
            }
            if (sourceRoot.isValid() && sourceRoot.getCanonicalFile() != null) {
                VfsUtil.processFilesRecursively(sourceRoot.getCanonicalFile(), virtualFile -> {
                    if (virtualFile.isDirectory()) {
                        return true;
                    }
                    PsiFile psiFile = originalFile.getManager().findFile(virtualFile);
                    if (psiFile == null) {
                        return true;
                    }
                    propertyCompletionProviders.stream()
                        .filter(p -> p.isValidFile(psiFile))
                        .forEach(p -> {
                            p.buildResultSet(resultSet, query, psiFile);
                        });
                    return true;
                });
            }
        }

        // prevent default property completion from running
        resultSet.stopHere();
    }

    private static @NotNull List<VirtualFile> getResourceRoots(Project project, PsiFile originalFile) {
        VirtualFile vf = originalFile.getVirtualFile();
        ProjectRootManager prm = ProjectRootManager.getInstance(project);
        ProjectFileIndex fileIndex = prm.getFileIndex();
        //TODO: we might have properties or cfg files in other modules (e.g. a separate karaf 'feature' module, containing .cfg files)
        if (fileIndex.isInTestSourceContent(vf)) {
            // only add test resources if the file we're running completion in is in a test root
            return prm.getModuleSourceRoots(Set.of(
                    JavaSourceRootType.SOURCE,
                    JavaResourceRootType.RESOURCE,
                    JavaSourceRootType.TEST_SOURCE,
                    JavaResourceRootType.TEST_RESOURCE
            ));
        } else {
            return prm.getModuleSourceRoots(Set.of(
                    JavaSourceRootType.SOURCE,
                    JavaResourceRootType.RESOURCE
            ));
        }
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, ProcessingContext context, CompletionQuery query) {
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        if (annotation != null && CamelIdeaUtils.PROPERTY_INJECT_ANNOTATION.equals(annotation.getQualifiedName())) {
            return true;
        }

        return CamelIdeaUtils.getService().hasUnclosedPropertyPlaceholder(query.valueAtPosition());
    }

}
