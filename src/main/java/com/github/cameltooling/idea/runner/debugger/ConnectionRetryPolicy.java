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

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.xdebugger.XDebugSession;

/**
 * Strategy deciding, for a {@link CamelDebuggerSession}, how long it is worth retrying a failed JMX connection
 * attempt and what should happen once that becomes a real problem rather than the debugged application still
 * starting up.
 */
interface ConnectionRetryPolicy {

    /**
     * @return {@code true} if a (re)connection attempt is still worth trying right now, {@code false} once there is
     * definitely nothing left to connect to (the local process ended, or the debug session was stopped).
     */
    boolean canRetry();

    /**
     * @return the number of consecutive failed attempts still considered a normal "the target is still starting up"
     * grace period, during which failures are expected and only logged at debug level.
     */
    int gracePeriodAttempts();

    /**
     * Called every time a connection attempt fails once {@code consecutiveFailedAttempts} has reached
     * {@link #gracePeriodAttempts()}, i.e. as soon as, and for as long as, a failure stops being explainable by the
     * target still starting up. It is up to the implementation to decide how often that is actually surfaced to the
     * user (e.g. once only, or as a periodic reminder while retrying keeps failing) so that a persistent problem
     * cannot go unnoticed for the rest of the session, without spamming on every single failed attempt.
     *
     * @param consecutiveFailedAttempts the total number of consecutive failed attempts so far, always
     *                                  {@code >= gracePeriodAttempts()}.
     * @param reason                    a human-readable explanation of the last failure.
     */
    void onGracePeriodExceeded(int consecutiveFailedAttempts, String reason);

    /**
     * Called when a connection attempt fails with a permanent error that retrying cannot possibly fix (wrong host,
     * authentication failure, malformed URL). The connection attempts are given up right after this call, so the
     * implementation must make sure the failure is surfaced to the user, unconditionally rather than only every so
     * often like {@link #onGracePeriodExceeded(int, String)} may do.
     *
     * @param reason a human-readable explanation of the failure.
     */
    void onPermanentFailure(String reason);

    /**
     * Reports a connection failure both as the usual balloon notification ({@link XDebugSession#reportError(String)})
     * and, when available, as a line in the Console tab of the Debug tool window, so the failure remains visible
     * after the balloon disappears rather than only for the few seconds it is shown.
     */
    static void reportError(XDebugSession xDebugSession, String reason) {
        xDebugSession.reportError(reason);
        if (xDebugSession.getConsoleView() instanceof ConsoleView consoleView) {
            consoleView.print(reason + "\n", ConsoleViewContentType.ERROR_OUTPUT);
        }
    }
}
