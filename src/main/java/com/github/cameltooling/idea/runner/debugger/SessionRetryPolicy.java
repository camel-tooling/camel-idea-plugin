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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;

/**
 * {@link ConnectionRetryPolicy} used when attaching to a genuinely remote, already running Camel process (a
 * dedicated "Camel Remote" run configuration, with no local {@link com.intellij.execution.process.ProcessHandler} to
 * watch).
 * <p/>
 * Unlike a process launched by the IDE, there is no legitimate "still starting up" phase to wait out here: the
 * target is assumed to already be running, so this mirrors the behavior of the built-in "Remote JVM Debug" run
 * configuration, which does not retry either — a single failed attempt is treated as a real, most likely permanent
 * problem (wrong host/port, unreachable network, or the Camel Debugger not being enabled on the target), and the
 * debug session is stopped rather than left polling forever.
 */
class SessionRetryPolicy implements ConnectionRetryPolicy {

    private static final Logger LOG = Logger.getInstance(SessionRetryPolicy.class);

    private static final int GRACE_PERIOD_ATTEMPTS = 1;

    private final XDebugSession xDebugSession;

    SessionRetryPolicy(XDebugSession xDebugSession) {
        this.xDebugSession = xDebugSession;
    }

    @Override
    public boolean canRetry() {
        return !xDebugSession.isStopped();
    }

    @Override
    public int gracePeriodAttempts() {
        return GRACE_PERIOD_ATTEMPTS;
    }

    @Override
    public void onGracePeriodExceeded(int consecutiveFailedAttempts, String reason) {
        // Stopping the session makes canRetry() return false right after, so this only ever runs once.
        LOG.warn(reason);
        xDebugSession.reportError(reason);
        xDebugSession.stop();
    }

    @Override
    public void onPermanentFailure(String reason) {
        // Same as giving up after the (single) grace attempt: report the failure and stop the session.
        onGracePeriodExceeded(0, reason);
    }
}
