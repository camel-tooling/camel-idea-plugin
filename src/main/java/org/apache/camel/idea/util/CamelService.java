/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea.util;

import java.util.HashSet;
import java.util.Set;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import org.jetbrains.annotations.NotNull;


/**
 * Service access for Camel libraries
 */
public class CamelService {

    Set<Library> processedLibraries = new HashSet<>();

    boolean camelPresent;

    /**
     * @return true if Camel is present on the classpath
     */
    public boolean isCamelPresent() {
        return camelPresent;
    }

    /**
     * @param camelPresent - true if camel is present
     */
    public void setCamelPresent(boolean camelPresent) {
        this.camelPresent = camelPresent;
    }

    /**
     * @param lib - Add library to the service library cache
     */
    public void addLibrary(Library lib){
        processedLibraries.add(lib);
    }

    /**
     * @return all cached libraries
     */
    public Set<Library> getLibraries() {
        return processedLibraries;
    }

    /**
     * Clean the library cache
     */
    public void clearLibraries() {
        processedLibraries.clear();
    }

    /**
     * @return true if the library is cached
     */
    public boolean containsLibrary(Library lib) {
        return processedLibraries.contains(lib);
    }

    /**
     * Scan for Camel Libraries and update the cache and isCamelPresent
     */
    public void scanForCamelDependencies(@NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                String name = libraryOrderEntry.getPresentableName().toLowerCase();
                if (name.contains("camel") && (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime())) {
                    if (!isCamelPresent() && name.contains("camel-core") && !name.contains("camel-core-") && libraryOrderEntry.getLibrary().getFiles(OrderRootType.CLASSES).length > 0) {
                        setCamelPresent(true);
                    }

                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    if (containsLibrary(library)) {
                        continue;
                    }
                    addLibrary(library);
                }
            }
        }
    }
}
