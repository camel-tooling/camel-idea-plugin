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
import com.intellij.xdebugger.XDebugSession;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProcessHandlerRetryPolicyTest {

    @Test
    public void testProcessHandlerRetryPolicy() {
        AtomicBoolean isTerminated = new AtomicBoolean(false);
        AtomicBoolean isTerminating = new AtomicBoolean(false);

        ProcessHandler mockProcessHandler = new ProcessHandler() {
            @Override
            protected void destroyProcessImpl() {}

            @Override
            protected void detachProcessImpl() {}

            @Override
            public boolean detachIsDefault() {
                return false;
            }

            @Override
            public java.io.OutputStream getProcessInput() {
                return null;
            }

            @Override
            public boolean isProcessTerminated() {
                return isTerminated.get();
            }

            @Override
            public boolean isProcessTerminating() {
                return isTerminating.get();
            }
        };

        XDebugSession mockSession = (XDebugSession) Proxy.newProxyInstance(
            XDebugSession.class.getClassLoader(),
            new Class<?>[]{XDebugSession.class},
            (proxy, method, args) -> null
        );

        ProcessHandlerRetryPolicy policy = new ProcessHandlerRetryPolicy(mockProcessHandler, mockSession);

        // 1. canRetry should be true if process is not terminated/terminating
        assertTrue(policy.canRetry());
        assertEquals(30, policy.gracePeriodAttempts());

        // 2. Terminating process means canRetry is false
        isTerminating.set(true);
        assertFalse(policy.canRetry());

        isTerminating.set(false);
        isTerminated.set(true);
        assertFalse(policy.canRetry());
    }

    @Test
    public void testOnGracePeriodExceededRemindsPeriodically() {
        ProcessHandler mockProcessHandler = new ProcessHandler() {
            @Override
            protected void destroyProcessImpl() {}
            @Override
            protected void detachProcessImpl() {}
            @Override
            public boolean detachIsDefault() { return false; }
            @Override
            public java.io.OutputStream getProcessInput() { return null; }
            @Override
            public boolean isProcessTerminated() { return false; }
            @Override
            public boolean isProcessTerminating() { return false; }
        };

        XDebugSession mockSession = (XDebugSession) Proxy.newProxyInstance(
            XDebugSession.class.getClassLoader(),
            new Class<?>[]{XDebugSession.class},
            (proxy, method, args) -> null
        );

        ProcessHandlerRetryPolicy policy = new ProcessHandlerRetryPolicy(mockProcessHandler, mockSession);

        // This method just logs and reports errors every REMINDER_INTERVAL_ATTEMPTS.
        // We can't easily verify static LOG calls, but we can verify it executes without errors.
        policy.onGracePeriodExceeded(30, "Grace period exceeded"); // 30 - 30 = 0 % 30 == 0
        policy.onGracePeriodExceeded(31, "Not reminded");
        policy.onGracePeriodExceeded(60, "Reminded again"); // 60 - 30 = 30 % 30 == 0
    }
}
