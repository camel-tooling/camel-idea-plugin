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

import java.io.IOException;

import eu.maveniverse.maven.mima.context.Runtimes;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The test class for {@link MavenArtifactRetrieverContext}.
 */
public class MavenArtifactRetrieverContextTest {

    private ClassLoader originalContextClassLoader;

    @Before
    public void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        // MIMA caches the resolved runtime in a static registry, clear it so that the lookup really happens
        Runtimes.INSTANCE.resetRuntimes();
    }

    @After
    public void tearDown() {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        Runtimes.INSTANCE.resetRuntimes();
    }

    /**
     * Reproduces #1269: MIMA looks its runtime up with {@code ServiceLoader}, so a context class loader that
     * cannot see the bundled runtime, as the platform class loader of the IDE, makes the build fail.
     */
    @Test
    public void shouldNotFindTheMavenRuntimeWithForeignContextClassLoader() throws IOException {
        Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());
        try (MavenDownloaderImpl downloader = new MavenDownloaderImpl()) {
            IllegalStateException exception = assertThrows(IllegalStateException.class, downloader::build);
            assertTrue(
                String.format("Unexpected failure: %s", exception.getMessage()),
                exception.getMessage().contains("No Runtime implementation found")
            );
        }
    }

    @Test
    public void shouldBuildWithForeignContextClassLoader() throws IOException {
        Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());
        try (MavenArtifactRetrieverContext context = new MavenArtifactRetrieverContext()) {
            assertNotNull(context.getClassLoader());
        }
    }

    @Test
    public void shouldRestoreTheContextClassLoader() throws IOException {
        ClassLoader expected = ClassLoader.getPlatformClassLoader();
        Thread.currentThread().setContextClassLoader(expected);
        try (MavenArtifactRetrieverContext ignored = new MavenArtifactRetrieverContext()) {
            assertNotNull(ignored);
        }
        assertSame(expected, Thread.currentThread().getContextClassLoader());
    }
}
