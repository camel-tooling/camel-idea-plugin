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

import java.net.UnknownHostException;

/**
 * A failure of a single Camel debugger connection attempt, classified by how it is worth reacting to. The concrete
 * type carries the <em>nature</em> of the failure; deciding how long and how often to keep retrying is left to the
 * {@link ConnectionRetryPolicy}.
 */
abstract sealed class CamelDebuggerConnectionException extends Exception
        permits CamelDebuggerConnectionException.Transient,
                CamelDebuggerConnectionException.Recoverable,
                CamelDebuggerConnectionException.Permanent {

    private CamelDebuggerConnectionException(String message) {
        super(message);
    }

    private CamelDebuggerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Classifies a connect-time failure. The actual cause is often wrapped several levels deep by the RMI or Jolokia
     * connector, so the whole cause chain is inspected: an {@link UnknownHostException} (wrong host) or a
     * {@link SecurityException} (authentication failure) cannot be fixed by retrying and is reported as
     * {@link Permanent}, while anything else (typically a connection refused because the target is not listening yet)
     * is assumed to be a {@link Transient} "not up yet" failure.
     *
     * @param jmxEndpoint a human-readable description of the JMX endpoint, used in the failure message.
     * @param failure     the exception raised while trying to obtain the connection.
     * @return the classified {@link CamelDebuggerConnectionException}.
     */
    static CamelDebuggerConnectionException fromConnectFailure(String jmxEndpoint, Exception failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownHostException) {
                return new Permanent("Unknown host for the JMX endpoint " + jmxEndpoint, failure);
            } else if (cause instanceof SecurityException) {
                return new Permanent("Authentication failed for the JMX endpoint " + jmxEndpoint, failure);
            }
        }
        return new Transient("Could not connect to the JMX endpoint " + jmxEndpoint, failure);
    }

    /**
     * The target could not be reached or is not ready yet (connection refused, or the BacklogDebugger MBean not
     * registered yet). Retrying is expected to eventually succeed once the debugged application has started up.
     */
    static final class Transient extends CamelDebuggerConnectionException {
        Transient(String message) {
            super(message);
        }

        Transient(String message, Throwable cause) {
            super(message, cause);
        }

        static Transient noBacklogDebuggerMBeanFound(String objectName) {
            return new Transient("No BacklogDebugger MBean found via " + objectName + ". "
                    + "Make sure camel-debug, camel-debug-starter or camel-quarkus-debug is on the classpath "
                    + "of the debugged application and that the Camel Debugger is enabled.");
        }
    }

    /**
     * A connection was successfully established but then lost while initializing the debugger (for instance the
     * application was restarted or redeployed mid-setup). The stale connection must be dropped and a new one opened.
     */
    static final class Recoverable extends CamelDebuggerConnectionException {
        Recoverable(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * The failure cannot possibly be fixed by retrying (wrong host, authentication failure, malformed URL), so there
     * is no point in keeping the connection attempts going.
     */
    static final class Permanent extends CamelDebuggerConnectionException {
        Permanent(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
