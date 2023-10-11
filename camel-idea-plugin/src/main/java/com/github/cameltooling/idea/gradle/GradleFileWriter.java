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

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.maven.model.Dependency;

/**
 * The supported writers of gradle files.
 */
public enum GradleFileWriter {
    /**
     * The writer dedicated to gradle files written in Kotlin.
     */
    KOTLIN {
        @Override
        public String getInitScriptFileName() {
            return "init.gradle.kts";
        }

        @Override
        String getListRepositoriesContent() {
            return """
                    allprojects {
                        tasks.register("cameltoolingShowRepos") {
                            repositories
                                .map{it as MavenArtifactRepository}
                                .forEach{ println("${it.name}=${it.url}")}
                        }
                    }
                    """;
        }
        @Override
        public void writeDependency(Writer writer, Dependency dependency) throws IOException {
            writer.write(
                String.format(
                    """
                    project.dependencies.add("runtimeOnly", "%s:%s:%s")
                    """,
                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                )
            );
        }

        @Override
        public void writeDependencies(Writer writer, List<Dependency> dependencies, String projectName) throws IOException {
            writer.write(
                String.format(
                    """
                    project("%s") {
                        plugins.withType<JavaPlugin>() {
                            dependencies {
                    """,
                    projectName
                )
            );
            for (Dependency dependency : dependencies) {
                writer.write(
                    String.format(
                        """
                                    add("runtimeOnly", "%s:%s:%s")
                        """,
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                    )
                );
            }
            writer.write(
                """
                        }
                    }
                }
                """
            );
        }

        @Override
        public void writeBuildFileLocation(Writer writer, Path buildFile) throws IOException {
            writer.write(
                String.format(
                    """
                    rootProject.buildFileName = "%s"
                    """,
                    buildFile.getFileName()
                )
            );
        }

        @Override
        public String getBuildScriptFileName() {
            return "build.gradle.kts";
        }

        @Override
        public String getSettingsScriptFileName() {
            return "settings.gradle.kts";
        }
    },
    /**
     * The writer dedicated to gradle files written in Groovy.
     */
    GROOVY {
        @Override
        public String getInitScriptFileName() {
            return "init.gradle";
        }

        @Override
        String getListRepositoriesContent() {
            return """
                    allprojects {
                        tasks.register('cameltoolingShowRepos') {
                            repositories
                                .collect{it as MavenArtifactRepository}
                                .forEach{ println("${it.name}=${it.url}")}
                        }
                    }
                    """;
        }
        @Override
        public void writeDependency(Writer writer, Dependency dependency) throws IOException {
            writer.write(
                String.format(
                    """
                    project.dependencies.add('runtimeOnly', '%s:%s:%s')
                    """,
                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                )
            );
        }

        @Override
        public void writeDependencies(Writer writer, List<Dependency> dependencies, String projectName) throws IOException {
            writer.write(
                String.format(
                    """
                    project('%s') {
                        plugins.withType(JavaPlugin.class) {
                            dependencies {
                    """,
                    projectName
                )
            );
            for (Dependency dependency : dependencies) {
                writer.write(
                    String.format(
                        """
                                    runtimeOnly '%s:%s:%s'
                        """,
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                    )
                );
            }
            writer.write(
                """
                        }
                    }
                }
                """
            );
        }

        @Override
        public void writeBuildFileLocation(Writer writer, Path buildFile) throws IOException {
            writer.write(
                String.format(
                    """
                    rootProject.buildFileName = '%s'
                    """,
                    buildFile.getFileName()
                )
            );
        }

        @Override
        public String getBuildScriptFileName() {
            return "build.gradle";
        }

        @Override
        public String getSettingsScriptFileName() {
            return "settings.gradle";
        }
    };


    /**
     * Writes the given dependency to the given writer.
     *
     * @param writer the target writer
     * @param dependency the dependency to write
     * @throws IOException if the dependency could not be written
     */
    public abstract void writeDependency(Writer writer, Dependency dependency) throws IOException;

    /**
     * Writes the given dependencies of a specific project to the given writer.
     *
     * @param writer the target writer
     * @param dependencies the dependencies to write
     * @param projectName the target project name
     * @throws IOException if the dependencies could not be written
     */
    public abstract void writeDependencies(Writer writer, List<Dependency> dependencies, String projectName) throws IOException;

    /**
     * Writes the location of the new build file.
     *
     * @param writer the target writer
     * @param buildFile the new build file to configure.
     * @throws IOException if the location of the build file could not be written
     */
    public abstract void writeBuildFileLocation(Writer writer, Path buildFile) throws IOException;

    /**
     * @return the name of the build script file
     */
    public abstract String getBuildScriptFileName();

    /**
     * @return the name of the settings script file
     */
    public abstract String getSettingsScriptFileName();

    /**
     * @return the name of the initialization script file
     */
    public abstract String getInitScriptFileName();

    /**
     * @return the code allowing to list the repositories of a Gradle project
     */
    abstract String getListRepositoriesContent();

    /**
     * Writes the code allowing to list the repositories of a Gradle project.
     *
     * @param writer the target writer
     * @throws IOException if the code could not be written
     */
    void writeListRepositoriesContent(Writer writer) throws IOException {
        writer.write(getListRepositoriesContent());
    }

    /**
     * @param vf the virtual file to check
     * @return the writer corresponding to the given virtual file, or {@code null} if none could be found
     */
    public static GradleFileWriter from(VirtualFile vf) {
        for (GradleFileWriter writer : values()) {
            if (vf.findChild(writer.getBuildScriptFileName()) != null || vf.findChild(writer.getSettingsScriptFileName()) != null) {
                return writer;
            }
        }
        return null;
    }

    /**
     * @param parent the parent directory to check
     * @return the writer corresponding to the given parent directory, or {@code null} if none could be found
     */
    public static GradleFileWriter from(Path parent) {
        for (GradleFileWriter writer : values()) {
            if (Files.exists(parent.resolve(writer.getBuildScriptFileName())) || Files.exists(parent.resolve(writer.getSettingsScriptFileName()))) {
                return writer;
            }
        }
        return null;
    }
}
