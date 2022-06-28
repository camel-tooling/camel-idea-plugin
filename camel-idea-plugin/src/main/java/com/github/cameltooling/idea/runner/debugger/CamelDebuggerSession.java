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

import com.github.cameltooling.idea.language.CamelLanguages;
import com.github.cameltooling.idea.runner.debugger.breakpoint.CamelBreakpoint;
import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.runner.debugger.util.ClasspathUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.AbstractDebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.sun.tools.attach.VirtualMachine;
import org.apache.camel.api.management.mbean.ManagedBacklogDebuggerMBean;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CamelDebuggerSession implements AbstractDebuggerSession {
    private static final Logger LOG = Logger.getInstance(CamelDebuggerSession.class);

    private static final int MAX_RETRIES = 30;
    private static final String BACKLOG_DEBUGGER_LOGGING_LEVEL = "TRACE";
    private static final long FALLBACK_TIMEOUT = Long.MAX_VALUE - 1;

    private final List<XLineBreakpoint<XBreakpointProperties<?>>> pendingBreakpointsAdd = new ArrayList<>();
    private final List<XLineBreakpoint<XBreakpointProperties<?>>> pendingBreakpointsRemove = new ArrayList<>();

    private final Map<String, CamelBreakpoint> breakpoints = new HashMap<>();
    private final List<String> explicitBreakpointIDs = new ArrayList<>();

    private final List<MessageReceivedListener> messageReceivedListeners = new ArrayList<>();

    private Project project;

    private ManagedBacklogDebuggerMBean backlogDebugger;
    private MBeanServerConnection serverConnection;
    private ObjectName debuggerMBeanObjectName;

    private org.w3c.dom.Document routesDOMDocument;

    private String temporaryBreakpointId;

    private XDebugSession xDebugSession;

    public boolean isConnected() {
        boolean isConnected = false;
        try {
            isConnected = backlogDebugger != null && backlogDebugger.isEnabled();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to know if the BacklogDebugger is enabled: {}", e.getMessage());
            }
        }
        return isConnected;
    }

    @Override
    public boolean isStopped() {
        return !backlogDebugger.isEnabled();
    }

    @Override
    public boolean isPaused() {
        return backlogDebugger.isSingleStepMode();
    }

    public void dispose() {
        disconnect();
    }

    public void connect(final ProcessHandler javaProcessHandler) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (connect(javaProcessHandler, true, 0)) {
                checkSuspendedBreakpoints();
            }
        });
    }

    public void disconnect() {
        if (backlogDebugger != null) {
            try {
                backlogDebugger.disableDebugger();
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not disable the BacklogDebugger: " + e.getMessage());
                }
            } finally {
                backlogDebugger = null;
                serverConnection = null;
            }
        }
    }

    public void addBreakpoint(XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint) {
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, true);
        } else {
            pendingBreakpointsAdd.add(xBreakpoint);
            pendingBreakpointsRemove.remove(xBreakpoint);
        }
    }

    public void removeBreakpoint(XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint) {
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, false);
        } else {
            pendingBreakpointsAdd.remove(xBreakpoint);
            pendingBreakpointsRemove.add(xBreakpoint);
        }
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void addMessageReceivedListener(MessageReceivedListener listener) {
        messageReceivedListeners.add(listener);
    }

    public void resume() {
        //Remove temporary breakpoint
        if (temporaryBreakpointId != null && !explicitBreakpointIDs.contains(temporaryBreakpointId)) {
            backlogDebugger.removeBreakpoint(temporaryBreakpointId);
        }
        temporaryBreakpointId = null;
        backlogDebugger.resumeAll();
    }

    public void setXDebugSession(XDebugSession xDebugSession) {
        this.xDebugSession = xDebugSession;
    }

    public XDebugSession getXDebugSession() {
        return xDebugSession;
    }

    public void setValue(String target,
                         @Nullable String targetName,
                         String expression,
                         String language,
                         String resultType,
                         @Nullable String bodyMediaType,
                         @Nullable String outputMediaType) {

        XSourcePosition position = xDebugSession.getCurrentPosition();
        Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);

        if (breakpointElement == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The breakpoint element could not be created from the position " + position);
            }
            return;
        }
        String breakpointId = breakpointElement.keySet().iterator().next();

        //First evaluate expression
        Map<String, String> params = new HashMap<>();
        params.put("resultType", resultType);
        if (!StringUtils.isEmpty(bodyMediaType)) {
            params.put("bodyMediaType", bodyMediaType);
        }
        if (!StringUtils.isEmpty(outputMediaType)) {
            params.put("outputMediaType", outputMediaType);
        }

        try {
            Object value = evaluateExpression(expression, language, params);
            if (value != null) {
                if ("Message Header".equals(target)) {
                    serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageHeaderOnBreakpoint",
                            new Object[]{breakpointId, targetName, value},
                            new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});
                } else if ("Exchange Property".equals(target)) {
                    serverConnection.invoke(this.debuggerMBeanObjectName, "setExchangePropertyOnBreakpoint",
                            new Object[]{breakpointId, targetName, value},
                            new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});
                } else if ("Body".equals(target)) {
                    serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageBodyOnBreakpoint",
                            new Object[]{breakpointId, value},
                            new String[]{"java.lang.String", "java.lang.Object"});
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not evaluate the expression " + expression, e);
        }
    }

    public Object evaluateExpression(String script, String language, @Nullable Map<String, String> params) {
        if (isConnected()) {
            XSourcePosition position = xDebugSession.getCurrentPosition();
            Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);

            if (breakpointElement == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The breakpoint element could not be created from the position " + position);
                }
                return null;
            }
            String breakpointId = breakpointElement.keySet().iterator().next();

            String stringClassName = String.class.getName();
            try {
                Object result;
                ClassLoader current = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(ClasspathUtils.getProjectClassLoader(project, this.getClass().getClassLoader()));

                    String bodyMediaType = params != null && params.containsKey("bodyMediaType") ? params.get("bodyMediaType") : "application/json";
                    String outputMediaType = params != null && params.containsKey("outputMediaType") ? params.get("outputMediaType") : "application/json";
                    String resultType = params != null && params.containsKey("resultType") ? params.get("resultType") : String.class.getName();

                    if (CamelLanguages.DatasonnetLanguage.LANGUAGE_ID.equalsIgnoreCase(language)) {
                        serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageHeaderOnBreakpoint", new Object[]{breakpointId, "CamelDatasonnetBodyMediaType", bodyMediaType},
                                new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});

                        serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageHeaderOnBreakpoint", new Object[]{breakpointId, "CamelDatasonnetOutputMediaType", outputMediaType},
                                new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});
                    }

                    result = serverConnection.invoke(this.debuggerMBeanObjectName, "evaluateExpressionAtBreakpoint",
                            new Object[]{breakpointId, language, script, resultType},
                            new String[]{stringClassName, stringClassName, stringClassName, stringClassName});

                    if (CamelLanguages.DatasonnetLanguage.LANGUAGE_ID.equalsIgnoreCase(language)) {
                        serverConnection.invoke(this.debuggerMBeanObjectName, "removeMessageHeaderOnBreakpoint", new Object[]{breakpointId, "CamelDatasonnetBodyMediaType"},
                                new String[]{"java.lang.String", "java.lang.String"});

                        serverConnection.invoke(this.debuggerMBeanObjectName, "removeMessageHeaderOnBreakpoint", new Object[]{breakpointId, "CamelDatasonnetOutputMediaType"},
                                new String[]{"java.lang.String", "java.lang.String"});
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(current);
                }
                return result;
            } catch (MBeanException | ReflectionException mbe) {
                return new Exception("Expression Evaluator is only available for Camel version 3.15 and later", mbe);
            } catch (Exception e) {
                return new Exception(e);
            }
        }
        return null;
    }

    public void stepInto(XSourcePosition position) {
        nextStep(position, false);
    }

    public void stepOver(XSourcePosition position) {
        nextStep(position, true);
    }

    public void stepOut(XSourcePosition position) {
        try {
            Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);

            if (breakpointElement == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The breakpoint element could not be created from the position " + position);
                }
                return;
            }
            String breakpointId = breakpointElement.keySet().iterator().next();

            //Get the route of the current tag
            Element routeElement = getParentRouteId(breakpointId);
            String routeId = routeElement.getAttribute("id");
            //Get current stack and find the caller in the stack
            List<CamelMessageInfo> stack = getStack(breakpointId, backlogDebugger.dumpTracedMessagesAsXml(breakpointId));
            CamelMessageInfo callerStackFrame = stack.stream()
                    .filter(info -> !info.getRouteId().equals(routeId) && info.getProcessorId().startsWith("to"))
                    .findFirst()
                    .orElse(null);
            if (callerStackFrame == null) { //This is the top route
                resume();
            } else {
                String newTemporaryBreakpointId = getSiblingId(callerStackFrame.getProcessorId());
                if (newTemporaryBreakpointId != null) {
                    //Add temporary breakpoint
                    backlogDebugger.addBreakpoint(newTemporaryBreakpointId);
                    //Run to that breakpoint
                    backlogDebugger.resumeBreakpoint(breakpointId);
                    if (temporaryBreakpointId != null
                            && !explicitBreakpointIDs.contains(temporaryBreakpointId)
                            && !temporaryBreakpointId.equals(newTemporaryBreakpointId)) { //Remove previous temporary breakpoint
                        backlogDebugger.removeBreakpoint(temporaryBreakpointId);
                    }
                    temporaryBreakpointId = newTemporaryBreakpointId;
                } else { //This was the last one
                    resume();
                }
            }

        } catch (Exception e) {
            LOG.warn("Could not process the step out at " + position, e);
        }
    }

    public void runToPosition(XSourcePosition fromPosition, XSourcePosition toPosition) {
        Map<String, PsiElement> fromBreakpointElement = createBreakpointElementFromPosition(fromPosition);
        Map<String, PsiElement> toBreakpointElement = createBreakpointElementFromPosition(toPosition);

        if (toBreakpointElement == null) { //this is not a tag
            if (LOG.isDebugEnabled()) {
                LOG.debug("The breakpoint element could not be created from the position " + toPosition);
            }
            return;
        }

        String toBreakpointId = toBreakpointElement.keySet().iterator().next();
        String fromBreakpointId = fromBreakpointElement.keySet().iterator().next();

        breakpoints.put(toBreakpointId, new CamelBreakpoint(toBreakpointId, toBreakpointElement.get(toBreakpointId), toPosition));

        backlogDebugger.addBreakpoint(toBreakpointId);
        //Run to that breakpoint
        backlogDebugger.resumeBreakpoint(fromBreakpointId);
        if (temporaryBreakpointId != null && !explicitBreakpointIDs.contains(temporaryBreakpointId) && !toBreakpointId.equals(temporaryBreakpointId)) { //Remove previous temporary breakpoint
            backlogDebugger.removeBreakpoint(temporaryBreakpointId);
        }
        temporaryBreakpointId = toBreakpointId;
    }

    private void nextStep(XSourcePosition position, boolean isOver) {
        Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);

        if (breakpointElement == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The breakpoint element could not be created from the position " + position);
            }
            return;
        }
        String breakpointId = breakpointElement.keySet().iterator().next();
        PsiElement breakpointTag = breakpointElement.get(breakpointId);
        breakpoints.put(breakpointId, new CamelBreakpoint(breakpointId, breakpointTag, position));

        String name = breakpointTag instanceof XmlTag ? ((XmlTag) breakpointTag).getLocalName() : breakpointTag.getText();

        if (isOver && ("to".equals(name) || "toD".equals(name))) {
            String newTemporaryBreakpointId = getSiblingId(breakpointId);
            if (newTemporaryBreakpointId != null) {
                //Add temporary breakpoint
                backlogDebugger.addBreakpoint(newTemporaryBreakpointId);
                //Run to that breakpoint
                backlogDebugger.resumeBreakpoint(breakpointId);
                if (temporaryBreakpointId != null
                        && !explicitBreakpointIDs.contains(temporaryBreakpointId)
                        && !newTemporaryBreakpointId.equals(temporaryBreakpointId)) { //Remove previous temporary breakpoint
                    backlogDebugger.removeBreakpoint(temporaryBreakpointId);
                }
                temporaryBreakpointId = newTemporaryBreakpointId;
            } else { //This was the last one
                resume();
            }
        } else {
            backlogDebugger.stepBreakpoint(breakpointId);
        }
    }

    private boolean connect(final ProcessHandler javaProcessHandler, boolean retry, int retries) {
        boolean isConnected = doConnect(javaProcessHandler);
        while (!isConnected && retry && retries < MAX_RETRIES && !javaProcessHandler.isProcessTerminated() && !javaProcessHandler.isProcessTerminating()) {
            try {
                Thread.sleep(1000L * 2);
                isConnected = connect(javaProcessHandler, retry, retries + 1);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return isConnected;
    }

    private boolean doConnect(final ProcessHandler javaProcessHandler) {
        String javaProcessPID = getPID(javaProcessHandler);
        ClassLoader current = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(ClasspathUtils.getProjectClassLoader(project, this.getClass().getClassLoader()));

            this.serverConnection = getLocalJavaProcessMBeanServer(javaProcessPID);
            if (serverConnection == null) {
                return false;
            }
            //init debugger
            // org.apache.camel:context=camel-1,type=tracer,name=BacklogDebugger
            ObjectName objectName = new ObjectName("org.apache.camel:context=*,type=tracer,name=BacklogDebugger");

            Set<ObjectName> names = serverConnection.queryNames(objectName, null);
            if (names != null && !names.isEmpty()) {
                this.debuggerMBeanObjectName = names.iterator().next();
                backlogDebugger = JMX.newMBeanProxy(serverConnection, debuggerMBeanObjectName, ManagedBacklogDebuggerMBean.class);
                backlogDebugger.enableDebugger();
                backlogDebugger.setLoggingLevel(BACKLOG_DEBUGGER_LOGGING_LEVEL); //By default it's INFO and a bit too noisy
                backlogDebugger.setFallbackTimeout(FALLBACK_TIMEOUT);
                //Lookup camel context
                objectName = new ObjectName("org.apache.camel:context=*,type=context,name=*");
                names = serverConnection.queryNames(objectName, null);
                if (names != null && !names.isEmpty()) {
                    ObjectName mbeanName = names.iterator().next();
                    ManagedCamelContextMBean camelContext = JMX.newMBeanProxy(serverConnection, mbeanName, ManagedCamelContextMBean.class);

                    while (!"Started".equals(camelContext.getState())) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Waiting for the context to start");
                        }
                        Thread.onSpinWait();
                    }

                    //Init DOM Documents
                    String routes = camelContext.dumpRoutesAsXml(false, true);
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                    InputStream targetStream = new ByteArrayInputStream(routes.getBytes());
                    this.routesDOMDocument = documentBuilder.parse(targetStream);

                    //TODO get list of loaded expression languages

                }

                //Toggle all pending breakpoints
                for (XLineBreakpoint<XBreakpointProperties<?>> breakpoint : pendingBreakpointsRemove) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            toggleBreakpoint(breakpoint, false);
                        } catch (Exception e) {
                            LOG.error(e);
                        }
                    });
                }
                for (XLineBreakpoint<XBreakpointProperties<?>> breakpoint : pendingBreakpointsAdd) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            toggleBreakpoint(breakpoint, true);
                        } catch (Exception e) {
                            LOG.error(e);
                        }
                    });
                }
                pendingBreakpointsAdd.clear();
                pendingBreakpointsRemove.clear();

                return true;
            }
            return false;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not initialize the BackLogDebugger", e);
            }
            return false;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    private MBeanServerConnection getLocalJavaProcessMBeanServer(String javaProcessPID) {
        try {
            final String localConnectorAddressProperty =
                    "com.sun.management.jmxremote.localConnectorAddress";
            VirtualMachine vm = VirtualMachine.attach(javaProcessPID);
            vm.startLocalManagementAgent();
            String connectorAddress =
                    vm.getAgentProperties().getProperty(localConnectorAddressProperty);
            JMXServiceURL jmxUrl = new JMXServiceURL(connectorAddress);
            MBeanServerConnection connection = JMXConnectorFactory.connect(jmxUrl).getMBeanServerConnection();
            vm.detach();
            return connection;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not retrieve the server connection of the java process " + javaProcessPID, e);
            }
            return null;
        }
    }

    private String getPID(ProcessHandler handler) {
        String cmdLine = handler.toString();
        for (ProcessInfo info : OSProcessUtil.getProcessList()) {
            if (info.getCommandLine().equals(cmdLine)) {
                return String.valueOf(info.getPid());
            }
        }
        return null;
    }

    private boolean toggleBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint, boolean toggleOn) {
        XSourcePosition position = xBreakpoint.getSourcePosition();
        Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);

        if (breakpointElement != null) {
            String breakpointId = breakpointElement.keySet().iterator().next();
            PsiElement psiElement = breakpointElement.get(breakpointId);
            if (toggleOn) {
                XExpression condition = xBreakpoint.getConditionExpression();
                if (condition == null) {
                    backlogDebugger.addBreakpoint(breakpointId);
                } else {
                    backlogDebugger.addConditionalBreakpoint(breakpointId, condition.getLanguage().getID(), condition.getExpression());
                }
                explicitBreakpointIDs.add(breakpointId);
            } else {
                backlogDebugger.removeBreakpoint(breakpointId);
                explicitBreakpointIDs.remove(breakpointId);
            }

            breakpoints.put(breakpointId, new CamelBreakpoint(breakpointId, psiElement, position));

            return true;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("The breakpoint element could not be created from the position %s", position));
        }
        //Breakpoint is invalid
        xDebugSession.setBreakpointInvalid(xBreakpoint, "Camel EIP ID not found");
        return false;
    }

    private void checkSuspendedBreakpoints() {
        while (isConnected()) {
            try {
                Collection<String> suspendedBreakpointIDs = (Collection<String>) serverConnection.invoke(
                    this.debuggerMBeanObjectName, "getSuspendedBreakpointNodeIds", new Object[]{}, new String[]{}
                );
                if (suspendedBreakpointIDs != null && !suspendedBreakpointIDs.isEmpty()) {
                    //Fire notifications here, we need to display the exchange, stack etc
                    for (String id : suspendedBreakpointIDs) {
                        final String suspendedMessage = dumpTracedMessagesAsXml(id);

                        ApplicationManager.getApplication().runReadAction(() -> {
                            for (MessageReceivedListener listener : messageReceivedListeners) {
                                try {
                                    CamelBreakpoint breakpoint = breakpoints.get(id);
                                    if (breakpoint == null) {
                                        //find tag and source position based on ID
                                        breakpoint = getCamelBreakpointById(id);
                                        breakpoints.put(id, breakpoint);
                                    }
                                    List<CamelMessageInfo> stack = getStack(id, suspendedMessage);
                                    CamelMessageInfo info = stack.get(0); //We only need stack for the top frame
                                    info.setStack(stack);
                                    listener.onNewMessageReceived(info);
                                } catch (Exception e) {
                                    LOG.warn("Could not notify the message listener", e);
                                }
                            }
                        });
                    }
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                LOG.warn("Could not check suspended breakpoints", e);
            }
        }
    }

    private String dumpTracedMessagesAsXml(String id) {
        String xml;
        try {
            // If the Camel version is 3.15 or later, the exchange properties are included
            xml = (String) serverConnection.invoke(this.debuggerMBeanObjectName, "dumpTracedMessagesAsXml", new Object[]{id, true},
                    new String[]{"java.lang.String", "boolean"});
        } catch (Exception e) {
            LOG.warn("Could not invoke dumpTracedMessagesAsXml(" + id + ", true)", e);
            // Could not invoke the dumpTracedMessagesAsXml with the new signature let's try the old one
            xml = backlogDebugger.dumpTracedMessagesAsXml(id);
        }
        return xml;
    }

    private Element getParentRouteId(String id) throws Exception {
        String path = "//route[*[attribute::id = '" + id + "']]";
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (Element) xPath.compile(path).evaluate(routesDOMDocument, XPathConstants.NODE);
    }

    private List<CamelMessageInfo> getStack(String breakpointId, String suspendedMessage) throws Exception {
        List<CamelMessageInfo> stack = new ArrayList<>();
        //Use new operation to retrieve message history
        String messageHistory = (String) serverConnection.invoke(this.debuggerMBeanObjectName, "messageHistoryOnBreakpointAsXml",
                new Object[]{breakpointId},
                new String[]{"java.lang.String"});

        InputStream targetStream = new ByteArrayInputStream(messageHistory.getBytes());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(targetStream);
        NodeList historyEntries = document.getElementsByTagName("messageHistoryEntry");

        for (int i = 0; i < historyEntries.getLength(); i++) {
            Element nextEntry = (Element) historyEntries.item(i);
            String routeId = nextEntry.getAttribute("routeId");
            String processorId = nextEntry.getAttribute("processorId");
            String processor = nextEntry.getAttribute("processor");
            CamelBreakpoint breakpoint = breakpoints.get(processorId);
            if (breakpoint == null) {
                //find tag and source position based on ID
                breakpoint = getCamelBreakpointById(processorId);
            }
            if (breakpoint != null) {
                breakpoints.put(processorId, breakpoint);
                CamelMessageInfo info = new CamelMessageInfo(suspendedMessage, breakpoint.getXSourcePosition(), breakpoint.getBreakpointTag(), routeId, processorId, processor, null);
                stack.add(info);
            }
        }

        Collections.reverse(stack);
        return stack;
    }

    private String getBreakpointId(@NotNull PsiElement breakpointTag) {
        String breakpointId = null;
        String sourceLocation = "";

        //Obtain file name and line number
        XSourcePosition position = XDebuggerUtil.getInstance().createPositionByElement(breakpointTag);
        int lineNumber = position.getLine() + 1; //Lines in XSourcePosition are 0-based

        final VirtualFile virtualFile = position.getFile();

        switch (virtualFile.getFileType().getName()) {
        case "XML":
        case "YAML":
            sourceLocation = virtualFile.getPresentableUrl();
            if (virtualFile.isInLocalFileSystem()) { //TODO - we need a better way to match source to target
                sourceLocation = String.format("file:%s", sourceLocation.replace("src/main/resources", "target/classes")); // file:/absolute/path/to/file.xml
            } else { //Then it must be a Jar
                sourceLocation = String.format("classpath:%s", sourceLocation.substring(sourceLocation.lastIndexOf("!") + 2));
            }
            break;
        case "JAVA":
            PsiClass psiClass = PsiTreeUtil.getParentOfType(breakpointTag, PsiClass.class);
            sourceLocation = psiClass.getQualifiedName();
            break;
        default: // noop
        }

        String path = "//*[@sourceLocation='" + sourceLocation + "' and @sourceLineNumber='" + lineNumber + "']";

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node breakpointTagFromContext = (Node) xPath.evaluate(path, routesDOMDocument, XPathConstants.NODE);
            if (breakpointTagFromContext != null) {
                breakpointId = breakpointTagFromContext.getAttributes().getNamedItem("id").getTextContent();
            }
        } catch (Exception e) {
            LOG.warn("Could not retrieve the breakpoint id from the path " + path, e);
        }

        return breakpointId;
    }

    @Nullable
    private CamelBreakpoint getCamelBreakpointById(String id) throws Exception {
        String path = "//*[@id='" + id + "']";
        //Find node with this ID in the document
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node tagNode = (Node) xPath.evaluate(path, routesDOMDocument, XPathConstants.NODE);
        if (tagNode == null) {
            return null;
        }
        Element tag = (Element) tagNode;
        String filePath = tag.getAttribute("sourceLocation");
        String lineNumber = tag.getAttribute("sourceLineNumber");

        if (StringUtils.isEmpty(filePath)) {
            return null;
        }
        if (StringUtils.isEmpty(lineNumber) || "-1".equals(lineNumber.trim())) {
            return null;
        }

        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

        if (!filePath.startsWith("file:") && !filePath.startsWith("classpath:")) { //This is Java class
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fileName, GlobalSearchScope.everythingScope(project));
            VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
            XSourcePosition position = XDebuggerUtil.getInstance().createPosition(virtualFile, new Integer(lineNumber) - 1);
            Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);
            return new CamelBreakpoint(id, breakpointElement.get(id), position);
        } else {
            PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, fileName, GlobalSearchScope.everythingScope(project));
            if (psiFiles.length < 1) {
                return null;
            }
            for (PsiFile psiFile : psiFiles) {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                String url = virtualFile.getPresentableUrl();
                if (virtualFile.isInLocalFileSystem()) { //TODO - we need a better way to match source to target
                    url = String.format("file:%s", url.replace("src/main/resources", "target/classes")); // file:/absolute/path/to/file.xml
                } else { //Then it must be a Jar
                    url = String.format("classpath:%s", url.substring(url.lastIndexOf("!") + 2));
                }
                if (filePath.equals(url)) {
                    //We found our file, let's get a source position
                    XSourcePosition position = XDebuggerUtil.getInstance().createPosition(virtualFile, Integer.parseInt(lineNumber) - 1);
                    Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);
                    return new CamelBreakpoint(id, breakpointElement.get(id), position);
                }
            }
        }

        return null;
    }

    @Nullable
    private Map<String, PsiElement> createBreakpointElementFromPosition(XSourcePosition position) {
        Map<String, PsiElement> breakpointElement = null;
        String breakpointId;
        PsiElement psiElement = null;

        VirtualFile file = position.getFile();
        switch (file.getFileType().getName()) {
        case "XML":
            psiElement = IdeaUtils.getXmlTagAt(project, position);
            break;
        case "JAVA":
            psiElement = XDebuggerUtil.getInstance().findContextElement(file, position.getOffset(), project, false);
            break;
        case "YAML":
            psiElement = IdeaUtils.getYamlKeyValueAt(project, position);
            if (psiElement != null) {
                psiElement = ((YAMLKeyValue) psiElement).getKey();
            }
            break;
        default: // noop
        }

        if (psiElement != null) {
            breakpointId = getBreakpointId(psiElement);
            if (breakpointId != null) {
                breakpointElement = Collections.singletonMap(breakpointId, psiElement);
            }
        }

        return breakpointElement;
    }

    @Nullable
    private String getSiblingId(String id) {
        //locate node in XML routes dump and get the next sibling
        String path = "//*[@id='" + id + "']";
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node tagNode;
        try {
            tagNode = (Node) xPath.evaluate(path, routesDOMDocument, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not retrieve the node from the path " + path, e);
            }
            tagNode = null;
        }
        if (tagNode == null) {
            return null;
        }
        Element tag = (Element) tagNode;
        Node sibling = tag.getNextSibling();
        while (null != sibling && sibling.getNodeType() != Node.ELEMENT_NODE) {
            sibling = sibling.getNextSibling();
        }
        if (sibling != null) {
            return ((Element) sibling).getAttribute("id");
        } else {
            Node parent = tag.getParentNode();
            while (null != parent && parent.getNodeType() != Node.ELEMENT_NODE) {
                parent = parent.getNextSibling();
            }
            if (parent != null && !parent.getNodeName().equals("route")) {
                Element parentElement = (Element) parent;
                return getSiblingId(parentElement.getAttribute("id"));
            }
        }
        return null;
    }
}
