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
package org.apache.camel.idea;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Smart completion for editing a Camel endpoint uri, to show a list of property holders can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class CamelSmartCompletionPropertyPlaceholders {
    //TODO Allow this to be configurable
    private static final List<String> IGNORE_PROPERTIES = Arrays.asList("java.", "Logger.", "logger", "appender.", "rootLogger.");

    public  void propertyPlaceholdersSmartCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        VfsUtil.processFilesRecursively(parameters.getOriginalFile().getManager().getProject().getBaseDir(), virtualFile -> {
            if (virtualFile.getName().endsWith(".properties")) {
                File file = new File(virtualFile.getPath());
                Properties properties = new Properties();
                try {
                    properties.load(Files.newInputStream(file.toPath()));
                } catch (IOException e) {
                }//TODO : log a warning, but for now we ignore it and continue.

                properties.forEach((key, value) -> {
                            String keyStr = (String) key;
                            boolean noneMatch = IGNORE_PROPERTIES.stream().noneMatch(s -> keyStr.startsWith(s));
                            if (noneMatch) {
                                LookupElementBuilder builder = LookupElementBuilder.create(keyStr+"}}")
                                        .appendTailText((String) value, true)
                                        .withPresentableText(keyStr + " = ");
                                resultSet.withPrefixMatcher(new PlainPrefixMatcher("")).addElement(builder);
                            }
                        }
                );
            }
            return true;
        });
    }
}
