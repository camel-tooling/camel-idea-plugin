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
package com.github.cameltooling.idea.service;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.github.cameltooling.idea.util.ArtifactCoordinates;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.apache.camel.tooling.maven.MavenDownloader;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;
import org.apache.camel.tooling.maven.MavenGav;
import org.apache.camel.tooling.maven.MavenResolutionException;

/**
 * {@code MavenArtifactRetrieverContext} is meant to be used to download artifacts from maven repositories.
 * All the downloaded artifacts and their dependencies are automatically added to the underlying
 * {@code URLClassLoader} to be able to have access to the local path of the artifacts.
 */
public class MavenArtifactRetrieverContext implements Closeable {

    private final MavenDownloader downloader;
    private final Map<String, String> repositories = new LinkedHashMap<>();
    private final MavenClassLoader classLoader = new MavenClassLoader();
    private final Map<ArtifactCoordinates, URL> allArtifacts = new HashMap<>();

    public MavenArtifactRetrieverContext() {
        this.downloader = new MavenDownloaderImpl();
        ((MavenDownloaderImpl) downloader).build();
    }

    /**
     * To add a 3rd party Maven repository.
     *
     * @param name the repository name
     * @param url  the repository url
     */
    public void addMavenRepository(String name, String url) {
        repositories.put(name, url);
    }

    /**
     * Downloads the artifact corresponding to the given coordinates and its dependencies, then adds them to
     * the {@link ClassLoader}.
     *
     * @param groupId the group id of the artifact to download
     * @param artifactId the artifact id of the artifact to download
     * @param version the version of the artifact to download
     * @throws IOException if an error occurs while downloading the artifact, or it could not
     * be added to the {@link ClassLoader}.
     */
    public void add(String groupId, String artifactId, String version) throws IOException {
        try {
            List<MavenArtifact> artifacts = downloader.resolveArtifacts(
                List.of(String.format("%s:%s:%s", groupId, artifactId, version)),
                new LinkedHashSet<>(repositories.values()), true, version.contains("SNAPSHOT")
            );
            for (MavenArtifact artifact : artifacts) {
                URL url = artifact.getFile().toURI().toURL();
                MavenGav gav = artifact.getGav();
                allArtifacts.put(ArtifactCoordinates.of(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()), url);
                classLoader.addURL(url);
            }
        } catch (MavenResolutionException e) {
            throw new IOException(e);
        }
    }

    public Map<ArtifactCoordinates, URL> getArtifacts() {
        return Collections.unmodifiableMap(allArtifacts);
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void close() throws IOException {
        classLoader.close();
    }

    private static class MavenClassLoader extends URLClassLoader {

        MavenClassLoader() {
            super(new URL[0]);
        }

        @Override
        protected void addURL(URL url) {
            super.addURL(url);
        }
    }
}
