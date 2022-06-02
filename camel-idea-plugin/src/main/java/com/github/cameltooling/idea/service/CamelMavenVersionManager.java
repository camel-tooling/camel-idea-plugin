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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.diagnostic.Logger;
import groovy.grape.Grape;
import groovy.lang.GroovyClassLoader;
import org.apache.camel.catalog.VersionManager;

/**
 * A copy of {@link org.apache.camel.catalog.maven.MavenVersionManager} as IDEA cannot use this class at runtime,
 * so we use a simpler copy here.
 */
class CamelMavenVersionManager implements VersionManager {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(CamelMavenVersionManager.class);
    private final ClassLoader classLoader = new GroovyClassLoader();
    private String version;
    private String runtimeProviderVersion;

    /**
     * To add a 3rd party Maven repository.
     *
     * @param name the repository name
     * @param url  the repository url
     */
    void addMavenRepository(String name, String url) {
        Map<String, Object> repo = new HashMap<>();
        repo.put("name", name);
        repo.put("root", url);
        Grape.addResolver(repo);
    }

    @Override
    public String getLoadedVersion() {
        return version;
    }

    @Override
    public boolean loadVersion(String version) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Trying to load the catalog version: " + version);
            }
            Grape.setEnableAutoDownload(true);

            final Map<String, Object> param = createMapDependency("org.apache.camel", "camel-catalog", version);
            param.put("classLoader", classLoader);

            Grape.grab(param);

            this.version = version;
            if (LOG.isDebugEnabled()) {
                LOG.debug("The catalog version " + version + " has been loaded");
            }
            return true;
        } catch (Exception e) {
            LOG.warn("Could not load the catalog version " + version + ": " + e.getMessage());
            LOG.debug(e);
            // ignore
            return false;
        }
    }

    @Override
    public String getRuntimeProviderLoadedVersion() {
        return runtimeProviderVersion;
    }

    @Override
    public boolean loadRuntimeProviderVersion(String groupId, String artifactId, String version) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Trying to load the runtime provider " + groupId + ":" + artifactId + ":" + version);
            }
            Grape.setEnableAutoDownload(true);

            final Map<String, Object> param = createMapDependency(groupId, artifactId, version);
            param.put("classLoader", classLoader);

            Grape.grab(param);

            this.runtimeProviderVersion = version;
            if (LOG.isDebugEnabled()) {
                LOG.debug("The runtime provider version " + groupId + ":" + artifactId + ":" + version + " has been loaded");
            }
            return true;
        } catch (Exception e) {
            // ignore
            LOG.warn(
                "Could not load the runtime provider " + groupId + ":" + artifactId + ":" + version + ": " + e.getMessage()
            );
            LOG.debug(e);
            return false;
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Trying to find the resource " + name + " from the catalog");
        }
        if (runtimeProviderVersion != null) {
            is = doGetResourceAsStream(name, runtimeProviderVersion);
        }
        if (is == null && version != null) {
            is = doGetResourceAsStream(name, version);
        }
        if (LOG.isDebugEnabled() && is == null) {
            LOG.debug("The resource " + name + " could not be found in the catalog");
        }
        return is;
    }

    private InputStream doGetResourceAsStream(String name, String version) {
        if (version == null) {
            return null;
        }

        try {
            URL found = null;
            Enumeration<URL> urls = classLoader.getResources(name);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getPath().contains(version)) {
                    found = url;
                    break;
                }
            }
            if (found != null) {
                return found.openStream();
            }
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @param groupId the group id of the dependency to download
     * @param artifactId the artifact id of the dependency to download
     * @param version the version of the dependency to download
     * @return the maven coordinates of the dependency to download as a {@code Map}.
     */
    private static Map<String, Object> createMapDependency(String groupId, String artifactId, String version) {
        final Map<String, Object> param = new HashMap<>();
        param.put("group", groupId);
        param.put("module", artifactId);
        param.put("version", version);
        return param;
    }
}

