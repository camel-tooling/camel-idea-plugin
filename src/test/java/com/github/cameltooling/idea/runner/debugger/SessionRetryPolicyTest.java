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

import com.intellij.xdebugger.XDebugSession;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionRetryPolicyTest {

    @Test
    public void testSessionRetryPolicy() {
        AtomicBoolean isStopped = new AtomicBoolean(false);
        AtomicBoolean stopCalled = new AtomicBoolean(false);

        XDebugSession mockSession = (XDebugSession) Proxy.newProxyInstance(
            XDebugSession.class.getClassLoader(),
            new Class<?>[]{XDebugSession.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isStopped")) {
                    return isStopped.get();
                } else if (method.getName().equals("stop")) {
                    stopCalled.set(true);
                    isStopped.set(true);
                    return null;
                } else if (method.getName().equals("getProject")) {
                    // Just return null for getProject to avoid NPE in ConnectionRetryPolicy.reportError
                    // if it uses the project. Actually, it uses xDebugSession.getProject() which might return null.
                    return null;
                }
                return null;
            }
        );

        SessionRetryPolicy policy = new SessionRetryPolicy(mockSession);
        
        // 1. Can retry while session is not stopped
        assertTrue(policy.canRetry());
        assertEquals(1, policy.gracePeriodAttempts());

        // 2. On grace period exceeded, it should stop the session
        policy.onGracePeriodExceeded(1, "Grace period failed");
        assertTrue(stopCalled.get());
        
        // 3. Once session is stopped, it can no longer retry
        assertFalse(policy.canRetry());
    }

    @Test
    public void testPermanentFailureStopsSession() {
        AtomicBoolean isStopped = new AtomicBoolean(false);
        AtomicBoolean stopCalled = new AtomicBoolean(false);

        XDebugSession mockSession = (XDebugSession) Proxy.newProxyInstance(
            XDebugSession.class.getClassLoader(),
            new Class<?>[]{XDebugSession.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isStopped")) {
                    return isStopped.get();
                } else if (method.getName().equals("stop")) {
                    stopCalled.set(true);
                    isStopped.set(true);
                    return null;
                }
                return null;
            }
        );

        SessionRetryPolicy policy = new SessionRetryPolicy(mockSession);
        policy.onPermanentFailure("Permanent fail");
        assertTrue(stopCalled.get());
        assertFalse(policy.canRetry());
    }
}
