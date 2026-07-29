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

/**
 * The transport used to reach the JMX endpoint exposing the Camel Debugger of a remote (or forked) Camel process.
 */
public enum JmxProtocol {

    /**
     * The traditional JMX over RMI registry, as exposed by {@code -Dcom.sun.management.jmxremote} or Spring
     * Boot/Quarkus dev-mode JMX registries.
     */
    RMI,
    /**
     * JMX tunneled over the Jolokia HTTP agent (e.g. exposed by {@code camel-management-jolokia} or the Spring Boot
     * Actuator Jolokia endpoint), useful whenever the RMI registry port cannot be reached (containers, firewalls).
     */
    JOLOKIA,
    /**
     * Custom protocol.
     */
    CUSTOM;

    /**
      * @param protocol chosen protocol.
      * @param host the host of the JMX endpoint.
      * @param port the port of the JMX endpoint.
      * @param useSsl whether the endpoint is reached over TLS. Only meaningful for {@link #JOLOKIA}, for which it must
      *               be non-{@code null}; for {@link #RMI} and {@link #CUSTOM} it must be {@code null}, since SSL is
      *               either configured differently (RMI) or already encoded in the user-supplied URL (CUSTOM).
      * @return the JMX service URL corresponding to this protocol for the given host/port, or {@code null} for
      *         {@link #CUSTOM} (whose URL is supplied by the user).
      * @throws IllegalArgumentException if {@code useSsl} does not match the given protocol.
     */
    public static String buildServiceUrl(JmxProtocol protocol, String host, int port, Boolean useSsl) {
        return switch (protocol) {
            case RMI -> {
                requireSslUnset(protocol, useSsl);
                yield "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi/camel".formatted(host, port);
            }
            case JOLOKIA -> {
                if (useSsl == null) {
                    throw new IllegalArgumentException("useSsl must be specified for the JOLOKIA protocol");
                }
                // Emit the explicit RFC 2609 scheme so the Jolokia agent never falls back to its port-based
                // https heuristic (a port ending in 443 would otherwise silently force TLS).
                yield "service:jmx:jolokia+%s://%s:%d/jolokia/".formatted(useSsl ? "https" : "http", host, port);
            }
            case CUSTOM -> {
                requireSslUnset(protocol, useSsl);
                yield null;
            }
        };
    }

    private static void requireSslUnset(JmxProtocol protocol, Boolean useSsl) {
        if (useSsl != null) {
            throw new IllegalArgumentException("useSsl is only applicable to the JOLOKIA protocol, not " + protocol);
        }
    }

    public static JmxProtocol getDefault() {
        return JmxProtocol.RMI;
    }

    // useSsl is unused today (Jolokia's default port is 8778 regardless of TLS) but is kept in the signature
    // for API symmetry with buildServiceUrl(), in case a future Jolokia deployment convention differs by scheme.
    public int getDefaultPort(@SuppressWarnings("unused") Boolean useSsl) {
        return switch (this) {
            case RMI     -> 1099;
            case JOLOKIA -> 8778;
            case CUSTOM  -> 0;
        };
    }
}
