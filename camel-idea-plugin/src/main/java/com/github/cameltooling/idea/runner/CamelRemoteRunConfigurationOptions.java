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

import java.util.Objects;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

public class CamelRemoteRunConfigurationOptions extends RunConfigurationOptions {

    private final StoredProperty<String> host = string("localhost").provideDelegate(this, "host");
    private final StoredProperty<Integer> port = property(1099).provideDelegate(this, "port");

    public String getHost() {
        return host.getValue(this);
    }

    public void setHost(String host) {
        this.host.setValue(this, host);
    }

    public Integer getPort() {
        return port.getValue(this);
    }

    public void setPort(Integer port) {
        this.port.setValue(this, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CamelRemoteRunConfigurationOptions that = (CamelRemoteRunConfigurationOptions) o;
        return Objects.equals(host, that.host) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, port);
    }
}
