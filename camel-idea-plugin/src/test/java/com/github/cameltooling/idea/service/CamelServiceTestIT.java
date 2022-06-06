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
package com.github.cameltooling.idea.service;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.exceptionCases.AbstractExceptionCase;

public class CamelServiceTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    public void testScanForCamelProjectShouldSupportDependenciesWithoutVersion() throws Throwable {
        CamelService service = getModule().getProject().getService(CamelService.class);
        assertNoException(ArrayIndexOutOfBoundsExceptionCase.check(
            () -> PsiTestUtil.addProjectLibrary(getModule(), "gradle::mylib:"))
        );
        assertNoException(ArrayIndexOutOfBoundsExceptionCase.check(
            () -> PsiTestUtil.addProjectLibrary(getModule(), "gradle:mygroup:myartifactId:"))
        );
        assertNoException(ArrayIndexOutOfBoundsExceptionCase.check(
            () -> PsiTestUtil.addProjectLibrary(getModule(), "mygroup:myartifactId:"))
        );
    }

}

abstract class ArrayIndexOutOfBoundsExceptionCase extends AbstractExceptionCase<ArrayIndexOutOfBoundsException> {

    @Override
    public Class<ArrayIndexOutOfBoundsException> getExpectedExceptionClass() {
        return ArrayIndexOutOfBoundsException.class;
    }

    static ArrayIndexOutOfBoundsExceptionCase check(Runnable runnable) {
        return new ArrayIndexOutOfBoundsExceptionCase() {
            @Override
            public void tryClosure() throws ArrayIndexOutOfBoundsException {
                runnable.run();
            }
        };
    }
}
