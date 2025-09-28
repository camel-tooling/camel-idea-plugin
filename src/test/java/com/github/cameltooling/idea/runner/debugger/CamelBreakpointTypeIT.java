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

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

public class CamelBreakpointTypeIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/runner/debugger";
    }

    @Override
    protected @Nullable String[] getMavenDependencies() {
        return new String[] {CAMEL_CORE_MODEL_MAVEN_ARTIFACT};
    }

    public void testCamelBreakpointPlacement() throws Exception {
        myFixture.configureByText(JavaFileType.INSTANCE,
                //language=JAVA
                """
                import org.apache.camel.builder.RouteBuilder;
                
                public static class CamelDebuggerTestRoute extends RouteBuilder {
                
                    @Override
                    public void configure() throws Exception { //line number: 5
                        from("timer:foo")                      //line number: 6
                            .to("log:start")                   //line number: 7
                            .process(e -> {                    //line number: 8
                                System.out.println(e);         //line number: 9
                            })                                 //line number: 10
                            .to("log:end");                    //line number: 11
                    }                                          //line nubmer: 12
                
                }
                """);

        XLineBreakpointType<?> camelBreakpointType = getCamelBreakpointType();

        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 5, false);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 6, false);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 7, true);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 8, true);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 9, false);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 10, false);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 11, true);
        assertThatBreakpointCanBePutAtLine(camelBreakpointType, 12, false);
    }

    private void assertThatBreakpointCanBePutAtLine(XLineBreakpointType<?> breakpoint, int line, boolean canBePut) {
        PsiFile psiFile = myFixture.getFile();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        Document document = myFixture.getEditor().getDocument();
        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        String lineText = document.getText().substring(lineStart, lineEnd).trim();
        String errorMessage = "Should " + (canBePut ? "" : "not ") + "be able to place Camel breakpoint on line " + line + ": '" + lineText + "'";
        assertEquals(errorMessage, canBePut, breakpoint.canPutAt(virtualFile, line, getProject()));
    }


    private static XLineBreakpointType<?> getCamelBreakpointType() {
        // Get the registered Camel breakpoint type through IDEA's extension mechanism
        XLineBreakpointType<?> camelBreakpointType = null;
        for (XLineBreakpointType<?> type : XDebuggerUtil.getInstance().getLineBreakpointTypes()) {
            if ("camel".equals(type.getId())) {
                camelBreakpointType = type;
                break;
            }
        }
        if (camelBreakpointType == null) {
            Assert.fail("No Camel breakpoint type found");
        }
        return camelBreakpointType;
    }

}
