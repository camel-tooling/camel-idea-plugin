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
package com.github.cameltooling.idea.completion;

import java.io.File;
import java.util.List;
import java.util.Objects;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.completion.JavaEndpointSmartCompletionTestIT.getJavaSourceKameletSuggestionsData;

/**
 * Testing smart completion with Kamelet endpoint in a project with custom Kamelet defined in maven jar, with a
 * specific catalog and with custom Kamelet defined in a resources directory.
 */
public class FullCustomKameletEndpointSmartCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected void loadDependencies(@NotNull ModifiableRootModel model) {
        super.loadDependencies(model);
        File rootFolder = new File("src/test/resources/testData/kamelet/kamelets-with-jar-catalog-resources/");
        PsiTestUtil.addLibrary(model, "com.foo:custom-kamelets:1.0", rootFolder.getPath(), "lib/custom-kamelets.jar");
        PsiTestUtil.addLibrary(model, "org.apache.camel.kamelets:camel-kamelets:0-SNAPSHOT", rootFolder.getPath(), "lib/specific-camel-kamelets.jar");
        VirtualFile resourcesDir = VirtualFileManager.getInstance().refreshAndFindFileByUrl(VfsUtil.getUrlForLibraryRoot(new File(rootFolder, "resources/")));
        PsiTestUtil.addResourceContentToRoots(
                model.getModule(),
                Objects.requireNonNull(resourcesDir),
            false
        );
    }

    /**
     * Ensure that the name of the custom Kamelets can be suggested
     */
    public void testKameletSuggestions() {
        myFixture.configureByText("CamelRoute.java", getJavaSourceKameletSuggestionsData());
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertDoesntContain(strings, "kamelet:xxx-source");
        assertContainsElements(strings, "kamelet:chuck-norris-in-jar-source", "kamelet:ftp-source", "kamelet:chuck-norris-in-resources");
    }
}
