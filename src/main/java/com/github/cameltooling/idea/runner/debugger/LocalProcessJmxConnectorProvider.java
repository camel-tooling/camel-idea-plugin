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

import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessInfo;
import com.sun.tools.attach.VirtualMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * {@link JmxConnectorProvider} that attaches to the local java process corresponding to the given
 * {@link ProcessHandler} by its process id. In case the process id cannot be found, it falls back to the given
 * {@code fallback} provider.
 */
class LocalProcessJmxConnectorProvider implements JmxConnectorProvider {

    private final ProcessHandler javaProcessHandler;
    private final JmxConnectorProvider fallback;

    LocalProcessJmxConnectorProvider(@NotNull ProcessHandler javaProcessHandler, JmxConnectorProvider fallback) {
        this.javaProcessHandler = javaProcessHandler;
        this.fallback = fallback;
    }

    @NotNull
    @Override
    public JMXConnector getJMXConnector() throws CamelDebuggerConnectionException {
        String javaProcessPID = getPID(javaProcessHandler);
        if (javaProcessPID == null) {
            return fallback.getJMXConnector();
        }
        try {
            VirtualMachine vm = VirtualMachine.attach(javaProcessPID);
            vm.startLocalManagementAgent();
            String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            vm.detach();
            return JMXConnectorFactory.connect(new JMXServiceURL(connectorAddress));
        } catch (Exception e) {
            throw CamelDebuggerConnectionException.fromConnectFailure("local process " + javaProcessPID, e);
        }
    }

    @Nullable
    private static String getPID(ProcessHandler handler) {
        String cmdLine = handler.toString();
        for (ProcessInfo info : OSProcessUtil.getProcessList()) {
            if (info.getCommandLine().equals(cmdLine)) {
                return String.valueOf(info.getPid());
            }
        }
        return null;
    }
}
