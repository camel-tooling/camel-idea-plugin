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
package com.github.cameltooling.idea.runner.beforerun;

import com.github.cameltooling.idea.runner.CamelRunConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;

public class CamelBeforeRunTasksProvider extends AbstractCamelBeforeRunTasksProvider {
    public static final Key<MavenBeforeRunTask> ID = Key.create("Camel.BeforeRunTask");

    @Override
    public Key getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Camel Builder";
    }

    @Override
    public String getDescription(CamelBeforeRunTask beforeRunTask) {
        return "Build the Camel Application";
    }

    @Nullable
    @Override
    public CamelBeforeRunTask createTask(RunConfiguration runConfiguration) {
        final CamelBeforeRunTask camelBeforeRunTask = new CamelBeforeRunTask(getId());
        camelBeforeRunTask.setEnabled(runConfiguration instanceof CamelRunConfiguration);
        return camelBeforeRunTask;
    }
}
