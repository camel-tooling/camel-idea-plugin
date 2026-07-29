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

import com.github.cameltooling.idea.runner.debugger.JmxProtocol;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

public class CamelRemoteRunConfigurationOptions extends RunConfigurationOptions {

    private final StoredProperty<String> host = string("").provideDelegate(this, "host");
    private final StoredProperty<Integer> port = property(0).provideDelegate(this, "port");
    private final StoredProperty<JmxProtocol> protocol = doEnum(JmxProtocol.getDefault(), JmxProtocol.class).provideDelegate(this, "protocol");

    /**
     * Whether the Jolokia endpoint is reached over TLS. Meaningful only when {@link #getProtocol()} is
     * {@link JmxProtocol#JOLOKIA}; ignored for the other protocols.
     */
    private final StoredProperty<Boolean> ssl = property(false).provideDelegate(this, "ssl");

    /**
     * The user-supplied JMX service URL, meaningful only when {@link #getProtocol()} is {@link JmxProtocol#CUSTOM}.
     */
    private final StoredProperty<String> serviceUrl = string("").provideDelegate(this, "serviceUrl");

    public String getHost() {
        return host.getValue(this);
    }

    public void setHost(String host) {
        this.host.setValue(this, host);
    }

    public String getResolvedHost() {
        String h = getHost();
        return h == null || h.isEmpty() ? "localhost" : h;
    }

    public Integer getPort() {
        return port.getValue(this);
    }

    public void setPort(Integer port) {
        this.port.setValue(this, port);
    }

    public JmxProtocol getProtocol() {
        return protocol.getValue(this);
    }

    public void setProtocol(JmxProtocol protocol) {
        this.protocol.setValue(this, protocol);
    }

    public int getResolvedPort() {
        int p = getPort();
        return p > 0 ? p : getProtocol().getDefaultPort(isSsl());
    }

    public boolean isSsl() {
        return ssl.getValue(this);
    }

    public void setSsl(boolean ssl) {
        this.ssl.setValue(this, ssl);
    }

    public String getServiceUrl() {
        return serviceUrl.getValue(this);
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl.setValue(this, serviceUrl == null ? "" : serviceUrl);
    }

    /**
     * @return {@link #getServiceUrl()} if {@link #getProtocol()} is {@link JmxProtocol#CUSTOM},
     * otherwise the JMX service URL derived from
     * {@link #getProtocol()}, {@link #getHost()} and {@link #getPort()}.
     */
    public String getEffectiveServiceUrl() {
        if (getProtocol() != JmxProtocol.CUSTOM) {
            Boolean useSsl = getProtocol() == JmxProtocol.JOLOKIA ? isSsl() : null;
            return JmxProtocol.buildServiceUrl(getProtocol(), getResolvedHost(), getResolvedPort(), useSsl);
        } else {
            return getServiceUrl();
        }
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
        return Objects.equals(host, that.host)
            && Objects.equals(port, that.port)
            && Objects.equals(protocol, that.protocol)
            && Objects.equals(ssl, that.ssl)
            && Objects.equals(serviceUrl, that.serviceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, port, protocol, ssl, serviceUrl);
    }
}
