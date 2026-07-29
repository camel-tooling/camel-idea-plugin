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

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ConnectionRetryPolicy} used when the debugged application was launched by the IDE and is therefore watched
 * through its {@link ProcessHandler} (whether the JMX connector itself is then obtained from the local process id or
 * from a service URL, in case of a process forked by a build tool such as Camel Quarkus or Camel Spring Boot).
 * <p/>
 * There is no network involved here (a local process id attach, or a service URL pointing at localhost for a
 * forked process), so a connection failure is never a network availability problem. It is instead either the Camel
 * context simply not being started yet, which a generous grace period accounts for, or, once that grace period is
 * exceeded, most likely a persistent, real problem such as the Camel Debugger component (camel-debug,
 * camel-debug-starter or camel-quarkus-debug) missing from the debugged application. Since the process is still
 * running, retrying continues in the background past the grace period, in case it was actually still starting up
 * after all, but the failure is reported again every {@link #REMINDER_INTERVAL_ATTEMPTS} attempts so that a
 * persistent problem cannot silently go unnoticed for the rest of the session.
 */
class ProcessHandlerRetryPolicy implements ConnectionRetryPolicy {

    private static final Logger LOG = Logger.getInstance(ProcessHandlerRetryPolicy.class);

    /**
     * At the usual 2-second interval between attempts, this gives the debugged application about a minute to start
     * up before a failure is considered worth reporting.
     */
    private static final int GRACE_PERIOD_ATTEMPTS = 30;

    /**
     * Once past the grace period, remind the user about the persisting failure about this often, roughly every
     * minute at the usual 2-second retry interval, instead of only once for the whole session.
     */
    private static final int REMINDER_INTERVAL_ATTEMPTS = 30;

    private final ProcessHandler javaProcessHandler;
    private final XDebugSession xDebugSession;

    ProcessHandlerRetryPolicy(@NotNull ProcessHandler javaProcessHandler, XDebugSession xDebugSession) {
        this.javaProcessHandler = javaProcessHandler;
        this.xDebugSession = xDebugSession;
    }

    @Override
    public boolean canRetry() {
        return !javaProcessHandler.isProcessTerminated() && !javaProcessHandler.isProcessTerminating();
    }

    @Override
    public int gracePeriodAttempts() {
        return GRACE_PERIOD_ATTEMPTS;
    }

    @Override
    public void onGracePeriodExceeded(int consecutiveFailedAttempts, String reason) {
        int attemptsPastGracePeriod = consecutiveFailedAttempts - GRACE_PERIOD_ATTEMPTS;
        if (attemptsPastGracePeriod % REMINDER_INTERVAL_ATTEMPTS == 0) {
            LOG.warn(reason);
            xDebugSession.reportError(reason);
        }
        // The local process is still running: keep retrying in the background, it might still catch up.
    }

    @Override
    public void onPermanentFailure(String reason) {
        LOG.warn(reason);
        ConnectionRetryPolicy.reportError(xDebugSession, reason);
        // Giving up on the polling is done by the caller; the local process itself is left running.
    }
}
