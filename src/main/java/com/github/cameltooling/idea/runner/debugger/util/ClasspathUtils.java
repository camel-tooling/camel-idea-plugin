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
package com.github.cameltooling.idea.runner.debugger.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public final class ClasspathUtils {
    private static final Key<ParameterizedCachedValue<List<URL>, Module>> URLS_KEY = Key.create("MODULE.URLS");
    private static final Logger LOG = Logger.getInstance(ClasspathUtils.class);

    private ClasspathUtils() {
    }

    public static ClassLoader getProjectClassLoader(Project project, ClassLoader parent) {
        List<URL> loaderUrls = new ArrayList<>();
        for (Module nextModule : ModuleManager.getInstance(project).getModules()) {
            loaderUrls.addAll(getURLsForModule(nextModule));
        }
        return new URLClassLoader(loaderUrls.toArray(new URL[0]), parent);
    }

    private static List<URL> getURLsForModule(Module module) {
        final CachedValuesManager manager = CachedValuesManager.getManager(module.getProject());
        return manager.getParameterizedCachedValue(module, URLS_KEY, new UrlsCachedProvider(), false, module);
    }

    private static class UrlsCachedProvider implements ParameterizedCachedValueProvider<List<URL>, Module> {
        @Nullable
        @Override
        public CachedValueProvider.Result<List<URL>> compute(Module module) {
            List<URL> loaderUrls = new ArrayList<>();

            ArrayList<Object> dependencies = new ArrayList<>();
            dependencies.add(ProjectRootManager.getInstance(module.getProject()));

            String fullClasspath = OrderEnumerator.orderEntries(module).recursively().getPathsList().getPathsString();

            String[] cpEntries = fullClasspath.split(":");
            for (String nextEntry : cpEntries) {
                try {
                    URL url = nextEntry.endsWith(".jar") ? new URL("jar:file://" + nextEntry + "!/") : new URL("file://" + nextEntry);
                    loaderUrls.add(url);
                } catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Could not add the entry %s due to: %s", nextEntry, e.getMessage()));
                    }
                }
            }

            CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
            String[] outputRootUrls = extension.getOutputRootUrls(false);
            for (String nextUrlString : outputRootUrls) {
                if (!nextUrlString.endsWith("/")) {
                    nextUrlString = nextUrlString + "/";
                }
                try {
                    loaderUrls.add(new URL(nextUrlString));
                } catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Could not add the URL %s due to: %s", nextUrlString, e.getMessage()));
                    }
                }
            }

            return CachedValueProvider.Result.create(loaderUrls, dependencies);
        }
    }
}
