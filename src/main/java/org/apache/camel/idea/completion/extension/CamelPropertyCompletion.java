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
package org.apache.camel.idea.completion.extension;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Completion handler for building property result set. Hook into the process when
 * scanning for property files and building the completion list in the {@link CamelPropertiesSmartCompletionExtension}
 */
public interface CamelPropertyCompletion {
    String[] IGNORE_PROPERTIES = {"java.", "Logger.", "logger", "appender.", "rootLogger.", "camel.springboot.*"};

    /**
     * @return true if it match the property file it should process
     */
    boolean isValidExtension(String filename);

    /**
     * Build the property completion result set to be shown in the completion dialog
     */
    void buildResultSet(CompletionResultSet resultSet, VirtualFile virtualFile);

    /**
     * Test if the property is on the ignore list
     */
    default boolean isIgnored(String key) {
        for (int i = 0; i < IGNORE_PROPERTIES.length; i++) {
            if (key.startsWith(IGNORE_PROPERTIES[i])) {
                return true;
            }
        }
        return false;
    }

}
