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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class JmxProtocolTest {

    @Test
    public void testRmiBuildServiceUrl() {
        String url = JmxProtocol.buildServiceUrl(JmxProtocol.RMI, "localhost", 1099, null);
        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi/camel", url);
    }

    @Test
    public void testRmiRejectsSsl() {
        assertThrows(IllegalArgumentException.class, () -> 
            JmxProtocol.buildServiceUrl(JmxProtocol.RMI, "localhost", 1099, true)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            JmxProtocol.buildServiceUrl(JmxProtocol.RMI, "localhost", 1099, false)
        );
    }

    @Test
    public void testJolokiaBuildServiceUrlHttp() {
        String url = JmxProtocol.buildServiceUrl(JmxProtocol.JOLOKIA, "127.0.0.1", 8778, false);
        assertEquals("service:jmx:jolokia+http://127.0.0.1:8778/jolokia/", url);
    }

    @Test
    public void testJolokiaBuildServiceUrlHttps() {
        String url = JmxProtocol.buildServiceUrl(JmxProtocol.JOLOKIA, "some.host.com", 443, true);
        assertEquals("service:jmx:jolokia+https://some.host.com:443/jolokia/", url);
    }

    @Test
    public void testJolokiaRequiresSslSet() {
        assertThrows(IllegalArgumentException.class, () -> 
            JmxProtocol.buildServiceUrl(JmxProtocol.JOLOKIA, "localhost", 8778, null)
        );
    }

    @Test
    public void testCustomBuildServiceUrl() {
        String url = JmxProtocol.buildServiceUrl(JmxProtocol.CUSTOM, "localhost", 1099, null);
        assertNull(url);
    }

    @Test
    public void testCustomRejectsSsl() {
        assertThrows(IllegalArgumentException.class, () -> 
            JmxProtocol.buildServiceUrl(JmxProtocol.CUSTOM, "localhost", 1099, true)
        );
    }

    @Test
    public void testDefaultPorts() {
        assertEquals(1099, JmxProtocol.RMI.getDefaultPort(null));
        assertEquals(8778, JmxProtocol.JOLOKIA.getDefaultPort(false));
        assertEquals(8778, JmxProtocol.JOLOKIA.getDefaultPort(true));
        assertEquals(0, JmxProtocol.CUSTOM.getDefaultPort(null));
    }
}
