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
package com.github.cameltooling.idea.gradle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

/**
 * {@code GradleUtil} is a utility class for Gradle related tasks.
 */
public final class GradleUtil {
    private static final Logger LOG = Logger.getInstance(GradleUtil.class);
    private GradleUtil() {
    }

    /**
     * Scans for third party maven repositories in the Gradle scripts files of the project.
     *
     * @param vf the root directory of the project
     * @return a map with repo id and url for each found repository. The map may be empty if no third party repository
     */
    public static Map<String, String> extractRepositoriesFomGradleProject(VirtualFile vf) {
        File initFile = createListRepositoriesInitScript(vf);
        if (initFile == null) {
            return Map.of();
        }
        Map<String, String> answer = new LinkedHashMap<>();
        try (ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(new File(vf.getCanonicalPath()))
            .connect();
             ByteArrayOutputStream stdOut = new ByteArrayOutputStream()) {
            connection.newBuild().withArguments("--init-script", initFile.getPath(), "-q", "cameltoolingShowRepos")
                .setStandardOutput(stdOut)
                .setStandardError(stdOut)
                .run();
            String[] lines = stdOut.toString().split("\n");
            for (String line : lines) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    LOG.info("Found third party Maven repository id: " + parts[0] + " url:" + parts[1]);
                    answer.put(parts[0], parts[1]);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error parsing Gradle project", e);
        }
        return answer;
    }

    /**
     * Creates a temporary init script file to list the repositories of the Gradle project.
     *
     * @param vf the root directory of the project
     * @return the created init script file or {@code null} if the file could not be created, or it is not
     * Gradle project
     */
    private static File createListRepositoriesInitScript(VirtualFile vf) {
        GradleFileWriter writer = GradleFileWriter.from(vf);
        if (writer == null) {
            return null;
        }

        File initFile = Paths.get(
            System.getProperty("java.io.tmpdir"), String.format("cameltooling.%s", writer.getInitScriptFileName())
        ).toFile();
        if (Files.exists(initFile.toPath()) && initFile.delete()) {
            LOG.debug("Deleted old init file: " + initFile);
        }
        try (FileWriter fileWriter = new FileWriter(initFile, false)) {
            writer.writeListRepositoriesContent(fileWriter);
        } catch (IOException e) {
            LOG.warn("Error parsing Gradle project", e);
            return null;
        }
        return initFile;
    }
}
