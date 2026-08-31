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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.github.cameltooling.idea.util.ArtifactCoordinates;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The integration test class for {@link MavenArtifactRetrieverContext}, resolving from Maven Central.
 */
public class MavenArtifactRetrieverContextIT {

    private static final String GROUP_ID = "org.apache.camel";
    private static final String ARTIFACT_ID = "camel-catalog";

    private static final String CAMEL_VERSION;

    static {
        final String projectRoot = new File(System.getProperty("user.dir")).getPath();
        try (InputStream is = new FileInputStream(projectRoot + "/gradle.properties")) {
            Properties gradleProperties = new Properties();
            gradleProperties.load(is);
            CAMEL_VERSION = gradleProperties.getProperty("camelVersion");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ClassLoader originalContextClassLoader;

    @Before
    public void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        Runtimes.INSTANCE.resetRuntimes();
    }

    @After
    public void tearDown() {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        Runtimes.INSTANCE.resetRuntimes();
    }

    /**
     * Unlike the creation of the context, the resolution does not rely on the context class loader. This test is
     * what confirms that it stays that way, see #1269.
     */
    @Test
    public void shouldResolveArtifactsWithForeignContextClassLoader() throws IOException {
        Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());
        try (MavenArtifactRetrieverContext context = new MavenArtifactRetrieverContext()) {
            context.add(GROUP_ID, ARTIFACT_ID, CAMEL_VERSION);

            Map<ArtifactCoordinates, URL> artifacts = context.getArtifacts();
            URL artifact = artifacts.get(ArtifactCoordinates.of(GROUP_ID, ARTIFACT_ID, CAMEL_VERSION));
            assertNotNull(
                String.format("%s:%s:%s could not be resolved, got %s", GROUP_ID, ARTIFACT_ID, CAMEL_VERSION, artifacts.keySet()),
                artifact
            );
            assertTrue(
                String.format("%s has not been added to the class loader", artifact),
                List.of(context.getClassLoader().getURLs()).contains(artifact)
            );
        }
    }
}
