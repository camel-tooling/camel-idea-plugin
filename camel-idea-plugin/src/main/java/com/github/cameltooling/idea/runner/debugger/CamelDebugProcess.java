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

import com.github.cameltooling.idea.runner.debugger.breakpoint.CamelBreakpointHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.util.ArrayUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

public class CamelDebugProcess extends XDebugProcess {
    private CamelDebuggerEditorsProvider camelDebuggerEditorsProvider;

    private CamelBreakpointHandler camelBreakpointHandler;
    private CamelDebuggerSession camelDebuggerSession;
    private ProcessHandler javaProcessHandler;

//  private final ExecutionConsole executionConsole;

    protected CamelDebugProcess(@NotNull XDebugSession session, @NotNull CamelDebuggerSession camelDebuggerSession, ProcessHandler javaProcessHandler) {
        super(session);

        this.camelDebuggerEditorsProvider = new CamelDebuggerEditorsProvider();
        this.camelDebuggerSession = camelDebuggerSession;
        this.javaProcessHandler = javaProcessHandler;
        this.camelBreakpointHandler = new CamelBreakpointHandler(session.getProject(), camelDebuggerSession);

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
        return camelDebuggerEditorsProvider;
    }

/*
  public CamelDebugProcess(@NotNull final XDebugSession session, @NotNull final MuleDebuggerSession muleDebuggerSession, ExecutionResult result, Map<String, String> modulesToAppsMap) {
    super(session);
    this.muleDebuggerSession = muleDebuggerSession;
    this.muleBreakpointHandler = new MuleBreakpointHandler(muleDebuggerSession, modulesToAppsMap);
    this.editorProperties = new MuleDebuggerEditorProperties();
    this.processHandler = result.getProcessHandler();
    this.executionConsole = result.getExecutionConsole();
    init();
  }
*/


    public void init() {
        camelDebuggerSession.connect(javaProcessHandler);
/*
    muleDebuggerSession.addMessageReceivedListener(new MessageReceivedListener() {
      @Override
      public void onNewMessageReceived(MuleMessageInfo muleMessageInfo) {

        getSession().positionReached(new MuleSuspendContext(new MuleStackFrame(getSession().getProject(), muleDebuggerSession, muleMessageInfo)));
      }

      @Override
      public void onExceptionThrown(MuleMessageInfo muleMessageInfo, ObjectFieldDefinition exceptionThrown) {

        getSession().positionReached(new MuleSuspendContext(new MuleStackFrame(getSession().getProject(), muleDebuggerSession, muleMessageInfo, exceptionThrown)));
      }

      @Override
      public void onExecutionStopped(MuleMessageInfo muleMessageInfo, List<ObjectFieldDefinition> frame, String path, String internalPosition) {
        System.out.println("MuleDebugProcess.onExecutionStopped : " + path + "#" + internalPosition);
        final WeaveIntegrationStackFrame weaveStackFrame = new WeaveIntegrationStackFrame(getSession().getProject(), muleDebuggerSession, path, internalPosition, frame);
        final MuleStackFrame muleStackFrame = new MuleStackFrame(getSession().getProject(), muleDebuggerSession, muleMessageInfo, null);
        getSession().positionReached(new MuleSuspendContext(weaveStackFrame, muleStackFrame));
      }

    });
*/
    }

    @Override
    public void stop() {
        camelDebuggerSession.disconnect();
    }

/*
    protected Project getProject() {
        return getSession().getProject();
    }
*/

    @NotNull
    @Override
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        final XBreakpointHandler<?>[] breakpointHandlers = super.getBreakpointHandlers();
        return ArrayUtil.append(breakpointHandlers, camelBreakpointHandler);
    }

//  @Override
//  public void startStepOver(@Nullable XSuspendContext context) {
//    muleDebuggerSession.nextStep();
//  }

/*
  @NotNull
  @Override
  public ExecutionConsole createConsole() {
    return executionConsole != null ? executionConsole : super.createConsole();
  }
*/

/*
  @Override
  protected ProcessHandler doGetProcessHandler() {
    return processHandler;
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    muleDebuggerSession.nextStep();
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    muleDebuggerSession.nextStep();
  }

  @Override
  public void resume(@Nullable XSuspendContext context) {
    muleDebuggerSession.resume();
  }

  @Override
  public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar, @NotNull DefaultActionGroup settings) {
    super.registerAdditionalActions(leftToolbar, topToolbar, settings);
    leftToolbar.add(new ExceptionBreakpointSwitchAction(muleDebuggerSession));
  }

  @Nullable
  @Override
  public XDebuggerEvaluator getEvaluator() {
    return new MuleScriptEvaluator(muleDebuggerSession);
  }

  @Override
  public void runToPosition(@NotNull XSourcePosition xSourcePosition, @Nullable XSuspendContext context) {
    //muleDebuggerSession.runToCursor(getMulePath(getXmlTagAt(getModule().getProject(), xSourcePosition)));
    muleDebuggerSession.runToCursor(getMulePath(getXmlTagAt(getProject(), xSourcePosition)));
  }




  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return editorProperties;
  }
*/

}
