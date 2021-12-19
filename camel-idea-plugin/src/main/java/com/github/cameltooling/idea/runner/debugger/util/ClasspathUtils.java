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

    private static ClasspathUtils classpathUtils = new ClasspathUtils();

    private ClasspathUtils() {

    }

    public static ClasspathUtils getInstance() {
        return classpathUtils;
    }
    public static ClassLoader getProjectClassLoader(Project project, ClassLoader parent) throws Exception {
        ClassLoader fullClassLoader = null;

        List<URL> loaderUrls = new ArrayList<>();

        Module[] modulesList = ModuleManager.getInstance(project).getModules();
        for (Module nextModule : modulesList) {
            loaderUrls.addAll(getURLsForModule(nextModule));
        }

        fullClassLoader = new URLClassLoader(loaderUrls.toArray(new URL[] {}), parent);

        return fullClassLoader;
    }

    public static ClassLoader getModuleClassLoader(Module module, ClassLoader parent) throws Exception {
        ClassLoader moduleClassLoader = null;

        List<URL> loaderUrls = getURLsForModule(module);

        moduleClassLoader = new URLClassLoader(loaderUrls.toArray(new URL[] {}), parent);

        return moduleClassLoader;
    }

    private static List<URL> getURLsForModule(Module module) throws Exception {

        final CachedValuesManager manager = CachedValuesManager.getManager(module.getProject());
        List<URL> loaderUrls = manager.getParameterizedCachedValue(module, URLS_KEY, new UrlsCachedProvider(), false, module);

        return loaderUrls;
    }

    private static class UrlsCachedProvider implements ParameterizedCachedValueProvider<List<URL>, Module> {
        @Nullable
        @Override
        public CachedValueProvider.Result<List<URL>> compute(Module module) {
            List<URL> loaderUrls = new ArrayList<>();

            ArrayList<Object> dependencies = new ArrayList<Object>();
            dependencies.add(ProjectRootManager.getInstance(module.getProject()));
            //dependencies.add(module);

            String fullClasspath = OrderEnumerator.orderEntries(module).recursively().getPathsList().getPathsString();

            String[] cpEntries = fullClasspath.split(":");
            for (String nextEntry : cpEntries) {
                try {
                    URL url = nextEntry.endsWith(".jar") ? new URL("jar:file://" + nextEntry + "!/") : new URL("file://" + nextEntry);
                    loaderUrls.add(url);
                } catch (Exception e) {

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

                }
            }

            return CachedValueProvider.Result.create(loaderUrls, dependencies);
        }
    }
}
