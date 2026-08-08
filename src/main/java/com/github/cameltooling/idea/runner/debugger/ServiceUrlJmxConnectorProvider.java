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
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * {@link JmxConnectorProvider} that always connects through a fixed JMX service URL. Used both for a genuinely
 * remote Camel process and for a process forked by the IDE (Camel Quarkus, Camel Spring Boot), for which a local
 * process id attach is not applicable.
 */
class ServiceUrlJmxConnectorProvider implements JmxConnectorProvider {

    private final String serviceUrl;

    ServiceUrlJmxConnectorProvider(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @NotNull
    @Override
    public JMXConnector getJMXConnector() throws CamelDebuggerConnectionException {
        try {
            return JMXConnectorFactory.connect(new JMXServiceURL(serviceUrl));
        } catch (Exception e) {
            throw CamelDebuggerConnectionException.fromConnectFailure(serviceUrl, e);
        }
    }
}
