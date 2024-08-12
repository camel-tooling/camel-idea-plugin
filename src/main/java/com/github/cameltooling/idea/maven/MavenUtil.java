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
package com.github.cameltooling.idea.maven;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.cameltooling.idea.gradle.GradleUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jetbrains.annotations.NotNull;

/**
 * {@code MavenUtil} is a utility class for Maven related tasks.
 */
public final class MavenUtil {
    private static final Logger LOG = Logger.getInstance(MavenUtil.class);

    private MavenUtil() {
    }

    /**
     * Scans for third party maven repositories in the root pom.xml file of the project.
     *
     * @return a map with repo id and url for each found repository. The map may be empty if no third party repository
     * is defined in the pom.xml file
     */
    static @NotNull Map<String, String> scanThirdPartyMavenRepositories(Project project) {
        VirtualFile vf = ProjectUtil.guessProjectDir(project);
        if (vf != null) {
            VirtualFile pom = vf.findFileByRelativePath("pom.xml");
            if (pom == null) {
                return GradleUtil.extractRepositoriesFomGradleProject(vf);
            }
            return extractFomPomFile(pom);
        }

        return Map.of();
    }

    /**
     * Scans for third party maven repositories in the root pom.xml file of the project.
     *
     * @param vf the root directory of the project
     * @return a map with repo id and url for each found repository. The map may be empty if no third party repository
     */
    private static Map<String, String> extractFomPomFile(VirtualFile vf) {
        Map<String, String> answer = new LinkedHashMap<>();
        try (InputStream is = vf.getInputStream()) {
            final Model model = new MavenXpp3Reader().read(is);
            for (Repository repository : model.getRepositories()) {
                String id = repository.getId();
                String url = repository.getUrl();
                if (id != null && url != null) {
                    LOG.info("Found third party Maven repository id: " + id + " url:" + url);
                    answer.put(id, url);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error parsing Maven pon.xml file", e);
        }
        return answer;
    }
}
