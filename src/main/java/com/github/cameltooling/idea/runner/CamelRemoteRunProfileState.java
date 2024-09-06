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
package com.github.cameltooling.idea.runner;

import com.intellij.debugger.engine.RemoteStateState;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.openapi.project.Project;

public class CamelRemoteRunProfileState extends RemoteStateState {

    private final CamelRemoteRunConfiguration configuration;

    CamelRemoteRunProfileState(Project project, CamelRemoteRunConfiguration configuration) {
        super(project, new RemoteConnection(true, configuration.getOptions().getHost(), Integer.toString(configuration.getOptions().getPort()), false));
        this.configuration = configuration;
    }

    public CamelRemoteRunConfiguration getConfiguration() {
        return configuration;
    }
}
