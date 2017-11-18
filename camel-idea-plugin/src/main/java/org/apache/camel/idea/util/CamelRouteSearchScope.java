/**
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
package org.apache.camel.idea.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import static org.apache.camel.idea.util.CamelIdeaUtils.CAMEL_FILE_EXTENSIONS;

/**
 * Search scope for Camel routes.
 */
public class CamelRouteSearchScope extends GlobalSearchScope {


    @Override
    public boolean contains(@NotNull VirtualFile virtualFile) {
        return virtualFile.isInLocalFileSystem() && isAllowedFileExtension(virtualFile.getExtension()) && isAllowedFile(virtualFile.getPresentableName());
    }

    @Override
    public int compare(@NotNull VirtualFile virtualFile, @NotNull VirtualFile virtualFile1) {
        return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@NotNull Module module) {
        return false;
    }

    @Override
    public boolean isSearchInLibraries() {
        return false;
    }

    private boolean isAllowedFileExtension(String extension) {
        for (String allowedExtension : CAMEL_FILE_EXTENSIONS) {
            if (allowedExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedFile(String fileName) {
        if (fileName.equals("pom.xml")) {
            return false;
        }
        return true;
    }
}
