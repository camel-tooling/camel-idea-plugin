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

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.completion.contributor.CamelContributor;
import org.jetbrains.annotations.NotNull;

/**
 * Camel Completion extension allow to extend the {@link CamelContributor} with additional smart
 * completion.
 * <p>
 * Adding a extension to the {@link CamelContributor} will be called when the smart completion
 * is processed.
 * </p>
 */
public interface CamelCompletionExtension {

    /**
     * Add a completion list to the exiting resultSet. Only called if the isValid return true;
     */
    void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet, @NotNull String[] query);

    /**
     * Validate if the extension should be executed.
     */
    boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, String[] query);

}
