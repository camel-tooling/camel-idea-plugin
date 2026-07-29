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
package com.github.cameltooling.idea.runner.debugger;

import org.jetbrains.annotations.NotNull;

import javax.management.remote.JMXConnector;

/**
 * Strategy for obtaining a {@link JMXConnector} to the JMX endpoint exposing the Camel Debugger.
 * <p/>
 * How a connector can be obtained differs depending on how the debugged Camel application is being run: a directly
 * launched local process can be attached to by its process id, while a genuinely remote process, or a process forked
 * by the IDE (such as Camel Quarkus or Camel Spring Boot), can only be reached through a JMX service URL.
 */
interface JmxConnectorProvider {

    /**
     * @return a connected {@link JMXConnector}.
     * @throws CamelDebuggerConnectionException if the connector could not be obtained, classified so the caller can
     *                                          decide whether it is worth retrying.
     */
    @NotNull
    JMXConnector getJMXConnector() throws CamelDebuggerConnectionException;
}
