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

import com.github.cameltooling.idea.language.DatasonnetLanguage;
import com.github.cameltooling.idea.runner.debugger.breakpoint.CamelBreakpoint;
import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.runner.debugger.util.ClasspathUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NonNavigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.AbstractDebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.sun.tools.attach.VirtualMachine;
import org.apache.camel.api.management.mbean.ManagedBacklogDebuggerMBean;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.intellij.plugins.xpathView.support.XPathSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    private static final int MAX_RETRIES = 30;
    private static final String BACKLOG_DEBUGGER_LOGGING_LEVEL = "TRACE";
    private static final long FALLBACK_TIMEOUT = Long.MAX_VALUE - 1;

    private final List<XLineBreakpoint<XBreakpointProperties>> pendingBreakpointsAdd = new ArrayList<>();
    private final List<XLineBreakpoint<XBreakpointProperties>> pendingBreakpointsRemove = new ArrayList<>();

    private final Map<String, CamelBreakpoint> breakpoints = new HashMap<>();
    private final List<String> explicitBreakpointIDs = new ArrayList<>();

    private final List<MessageReceivedListener> messageReceivedListeners = new ArrayList<>();

    private Project project;
    private ManagedBacklogDebuggerMBean backlogDebugger;
    private ManagedCamelContextMBean camelContext;

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
            boolean isConnected = connect(javaProcessHandler, true, 0);
            if (isConnected) {
                checkSuspendedBreakpoints();
            }
        });
    }

    public void disconnect() {
        if (backlogDebugger != null) {
            try {
                backlogDebugger.disableDebugger();
            } catch (Exception e) {
            } finally {
                backlogDebugger = null;
                serverConnection = null;
                camelContext = null;
            }
        }
    }

    public void addBreakpoint(XLineBreakpoint<XBreakpointProperties> xBreakpoint) {
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, true);
        } else {
            pendingBreakpointsAdd.add(xBreakpoint);
            pendingBreakpointsRemove.remove(xBreakpoint);
        }
    }

    public void removeBreakpoint(XLineBreakpoint<XBreakpointProperties> xBreakpoint) {
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, false);
        } else {
            pendingBreakpointsAdd.remove(xBreakpoint);
            pendingBreakpointsRemove.add(xBreakpoint);
        }
    }

    public ManagedCamelContextMBean getCamelContext() {
        return camelContext;
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

    public Object evaluateExpression(String script, String language, @Nullable Map<String, String> params) {
        if (isConnected()) {
            XSourcePosition xSourcePosition = xDebugSession.getCurrentPosition();
            XmlTag breakpointTag = IdeaUtils.getService().getXmlTagAt(project, xSourcePosition);
            String breakpointId = getBreakpointId(breakpointTag);

            String stringClassName = String.class.getName();
            try {
                Object result;
                ClassLoader current = Thread.currentThread().getContextClassLoader();
                try {
                    ClassLoader projectClassLoader = ClasspathUtils.getProjectClassLoader(project, this.getClass().getClassLoader());
                    Thread.currentThread().setContextClassLoader(projectClassLoader);

                    String bodyMediaType = params != null && params.containsKey("bodyMediaType") ? params.get("bodyMediaType") : "application/json";
                    String outputMediaType = params != null && params.containsKey("outputMediaType") ? params.get("outputMediaType") : "application/json";
                    String resultType = params != null && params.containsKey("resultType") ? params.get("resultType") : String.class.getName();

                    if (DatasonnetLanguage.LANGUAGE_ID.equals(language)) {
                        serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageHeaderOnBreakpoint", new Object[]{breakpointId, "CamelDatasonnetBodyMediaType", bodyMediaType},
                                new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});

                        serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageHeaderOnBreakpoint", new Object[]{breakpointId, "CamelDatasonnetOutputMediaType", outputMediaType},
                                new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});
                    }

                    result = serverConnection.invoke(this.debuggerMBeanObjectName, "evaluateExpressionAtBreakpoint",
                            new Object[]{breakpointId, language, script, resultType},
                            new String[]{stringClassName, stringClassName, stringClassName, stringClassName});

                    if (DatasonnetLanguage.LANGUAGE_ID.equals(language)) {
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
            XmlTag currentTag = IdeaUtils.getService().getXmlTagAt(project, position);
            String breakpointId = getBreakpointId(currentTag);
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
                //Find breakpoint tag
                XmlTag nextTag = PsiTreeUtil.getNextSiblingOfType(callerStackFrame.getTag(), XmlTag.class);
                if (nextTag != null) {
                    //Add temporary breakpoint
                    String newTemporaryBreakpointId = getBreakpointId(nextTag);
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
            e.printStackTrace();
        }
    }

    public void runToPosition(XSourcePosition fromPosition, XSourcePosition toPosition) {

        XmlTag toBreakpointTag = IdeaUtils.getService().getXmlTagAt(project, toPosition);
        if (toBreakpointTag == null) { //this is not a tag
            return;
        }
        String toBreakpointId = getBreakpointId(toBreakpointTag);
        if (toBreakpointId == null) { //this is not a tag
            return;
        }
        XmlTag fromBreakpointTag = IdeaUtils.getService().getXmlTagAt(project, fromPosition);
        String fromBreakpointId = getBreakpointId(fromBreakpointTag);

        breakpoints.put(toBreakpointId, new CamelBreakpoint(toBreakpointId, toBreakpointTag, toPosition));

        backlogDebugger.addBreakpoint(toBreakpointId);
        //Run to that breakpoint
        backlogDebugger.resumeBreakpoint(fromBreakpointId);
        if (temporaryBreakpointId != null && !explicitBreakpointIDs.contains(temporaryBreakpointId) && !toBreakpointId.equals(temporaryBreakpointId)) { //Remove previous temporary breakpoint
            backlogDebugger.removeBreakpoint(temporaryBreakpointId);
        }
        temporaryBreakpointId = toBreakpointId;
    }

    private void nextStep(XSourcePosition position, boolean isOver) {
        XmlTag breakpointTag = IdeaUtils.getService().getXmlTagAt(project, position);
        String breakpointId = getBreakpointId(breakpointTag);
        breakpoints.put(breakpointId, new CamelBreakpoint(breakpointId, breakpointTag, position));

        if (isOver && ("to".equals(breakpointTag.getLocalName()) || "toD".equals(breakpointTag.getLocalName()))) {
            XmlTag nextTag = PsiTreeUtil.getNextSiblingOfType(breakpointTag, XmlTag.class);
            if (nextTag != null) {
                //Add temporary breakpoint
                String newTemporaryBreakpointId = getBreakpointId(nextTag);
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
                return false;
            }
        }
        return isConnected;
    }

    private boolean doConnect(final ProcessHandler javaProcessHandler) {
        String javaProcessPID = getPID(javaProcessHandler);
        ClassLoader current = Thread.currentThread().getContextClassLoader();

        try {
            ClassLoader projectClassLoader = ClasspathUtils.getProjectClassLoader(project, this.getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(projectClassLoader);

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
                    camelContext = JMX.newMBeanProxy(serverConnection, mbeanName, ManagedCamelContextMBean.class);

                    //Init DOM Document
                    String routes = getCamelContext().dumpRoutesAsXml(false, true);
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                    InputStream targetStream = new ByteArrayInputStream(routes.getBytes());
                    this.routesDOMDocument = documentBuilder.parse(targetStream);
                }

                //Toggle all pending breakpoints
                for (XLineBreakpoint breakpoint : pendingBreakpointsAdd) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        toggleBreakpoint(breakpoint, true);
                    });
                }
                for (XLineBreakpoint breakpoint : pendingBreakpointsRemove) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        toggleBreakpoint(breakpoint, false);
                    });
                }
                pendingBreakpointsAdd.clear();
                pendingBreakpointsRemove.clear();

                return true;
            }
            return false;
        } catch (Exception e) {
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
            return null;
        }
    }

    private String getPID(ProcessHandler handler) {
        String cmdLine = handler.toString();
        ProcessInfo[] infoList = OSProcessUtil.getProcessList();
        for (ProcessInfo info : infoList) {
            if (info.getCommandLine().equals(cmdLine)) {
                return String.valueOf(info.getPid());
            }
        }
        return null;
    }

    private String getBreakpointId(XmlTag breakpointTag) {
        String breakpointId = null;
        String path = "/routes" + getXPathOfTheXMLTag(breakpointTag);

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node breakpointTagFromContext = (Node) xPath.compile(path).evaluate(routesDOMDocument, XPathConstants.NODE);
            if (breakpointTagFromContext != null) {
                breakpointId = breakpointTagFromContext.getAttributes().getNamedItem("id").getTextContent();
            }
        } catch (Exception e) {
            breakpointId = null;
            e.printStackTrace();
        }
        return breakpointId;
    }

    private boolean toggleBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> xBreakpoint, boolean toggleOn) {

        XSourcePosition position = xBreakpoint.getSourcePosition();

        XmlTag breakpointTag = IdeaUtils.getService().getXmlTagAt(project, position);
        String breakpointId = getBreakpointId(breakpointTag);

        if (breakpointId != null) {
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

            breakpoints.put(breakpointId, new CamelBreakpoint(breakpointId, breakpointTag, position));

            return true;
        }

        return false;
    }

    private void checkSuspendedBreakpoints() {
        while (isConnected()) {
            try {
                //Set<String> suspendedBreakpointIDs = backlogDebugger.getSuspendedBreakpointNodeIds();
                // this throws exception: javax.management.AttributeNotFoundException: getAttribute failed: ModelMBeanAttributeInfo not found for SuspendedBreakpointNodeIds
                //  at java.management/javax.management.modelmbean.RequiredModelMBean.getAttribute(RequiredModelMBean.java:1440)
                //  at java.management/com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.getAttribute(DefaultMBeanServerInterceptor.java:641)
                //  at java.management/com.sun.jmx.mbeanserver.JmxMBeanServer.getAttribute(JmxMBeanServer.java:678)
                //  at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl.doOperation(RMIConnectionImpl.java:1443)
                //  at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl$PrivilegedOperation.run(RMIConnectionImpl.java:1307)
                //  at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl.doPrivilegedOperation(RMIConnectionImpl.java:1399)
                //  at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl.getAttribute(RMIConnectionImpl.java:637)
                //  at java.base/jdk.internal.reflect.GeneratedMethodAccessor151.invoke(Unknown Source)
                //  at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                //  at java.base/java.lang.reflect.Method.invoke(Method.java:567)
                //  at java.rmi/sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:359)
                //  at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:200)
                //  at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:197)
                //  at java.base/java.security.AccessController.doPrivileged(AccessController.java:689)
                //  at java.rmi/sun.rmi.transport.Transport.serviceCall(Transport.java:196)
                //  at java.rmi/sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:562)
                //  at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:796)
                //  at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:677)
                //  at java.base/java.security.AccessController.doPrivileged(AccessController.java:389)
                //  at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:676)
                //  at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
                //  at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
                //  at java.base/java.lang.Thread.run(Thread.java:835)
                //  at java.rmi/sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:303)
                //  at java.rmi/sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:279)
                //  at java.rmi/sun.rmi.server.UnicastRef.invoke(UnicastRef.java:164)
                //  at jdk.remoteref/jdk.jmx.remote.internal.rmi.PRef.invoke(Unknown Source)
                //  at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl_Stub.getAttribute(Unknown Source)
                //  at java.management.rmi/javax.management.remote.rmi.RMIConnector$RemoteMBeanServerConnection.getAttribute(RMIConnector.java:904)
                //  at java.management/javax.management.MBeanServerInvocationHandler.invoke(MBeanServerInvocationHandler.java:273)
                //  ... 13 more

                Collection<String> suspendedBreakpointIDs = (Collection<String>) serverConnection.invoke(this.debuggerMBeanObjectName, "getSuspendedBreakpointNodeIds", new Object[]{}, new String[]{});
                if (suspendedBreakpointIDs != null && !suspendedBreakpointIDs.isEmpty()) {
                    //Fire notifications here, we need to display the exchange, stack etc
                    for (String id : suspendedBreakpointIDs) {
                        String xml = backlogDebugger.dumpTracedMessagesAsXml(id);
                        try { //If the Camel version is 3.15 or later, the exchange properties are included
                            xml = (String) serverConnection.invoke(this.debuggerMBeanObjectName, "dumpTracedMessagesAsXml", new Object[]{id, true},
                                    new String[]{"java.lang.String", "boolean"});
                        } catch (Exception e) {
                            //TODO log this or display warning
                        }
                        final String suspendedMessage = xml;

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
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {

                } finally {

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    private CamelBreakpoint getCamelBreakpointById(String id) throws Exception {
        String path = "//*[@id='" + id + "']";
        //Find node with this ID in the document
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node tagNode = (Node) xPath.compile(path).evaluate(routesDOMDocument, XPathConstants.NODE);
        if (tagNode == null) {
            return null;
        }
        Element tag = (Element) tagNode;
        //Find XPath of this tag that we can use to locate it in the source code
        String camelXPath = getXPathOfTheElement(tag);

        final XPathSupport support = XPathSupport.getInstance();

        final Collection<VirtualFile> files = FileTypeIndex.getFiles(XmlFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        for (VirtualFile file : files) {
            ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
            if (index.getSourceRootForFile(file) != null || index.getClassRootForFile(file) != null) { //Only in source root or classpath
                final XmlFile xmlFile = (XmlFile) PsiManager.getInstance(project).findFile(file);
                final org.jaxen.XPath nextXpath = support.createXPath(xmlFile, camelXPath);
                final Object result = nextXpath.evaluate(xmlFile.getRootTag());
                if (result != null && result instanceof List<?>) {
                    final List<?> list = (List<?>) result;
                    if (!((List<?>) result).isEmpty()) {
                        XmlTag xmlTag = (XmlTag) list.get(0);
                        final SmartPsiElementPointer<PsiElement> pointer =
                                SmartPointerManager.getInstance(xmlTag.getProject()).createSmartPsiElementPointer(xmlTag);
                        XSourcePosition position = new XSourcePosition() {
                            private volatile XSourcePosition myDelegate;

                            private XSourcePosition getDelegate() {
                                if (myDelegate == null) {
                                    myDelegate = ApplicationManager.getApplication().runReadAction(new Computable<XSourcePosition>() {
                                        @Override
                                        public XSourcePosition compute() {
                                            PsiElement elem = pointer.getElement();
                                            return XSourcePositionImpl.createByOffset(pointer.getVirtualFile(), elem != null ? elem.getTextOffset() : -1);
                                        }
                                    });
                                }
                                return myDelegate;
                            }

                            @Override
                            public int getLine() {
                                return getDelegate().getLine();
                            }

                            @Override
                            public int getOffset() {
                                return getDelegate().getOffset();
                            }

                            @NotNull
                            @Override
                            public VirtualFile getFile() {
                                return file;
                            }

                            @NotNull
                            @Override
                            public Navigatable createNavigatable(@NotNull Project project) {
                                // no need to create delegate here, it may be expensive
                                if (myDelegate != null) {
                                    return myDelegate.createNavigatable(project);
                                }
                                PsiElement elem = pointer.getElement();
                                if (elem instanceof Navigatable) {
                                    return (Navigatable) elem;
                                }
                                return NonNavigatable.INSTANCE;
                            }
                        };
                        return new CamelBreakpoint(id, xmlTag, position);
                    }
                }
            }
        }

        return null;
    }

    private String getXPathOfTheXMLTag(XmlTag tag) {
        String path = tag.getName();
        if ("route".equals(path)) {
            //First try to obtain ID
            String id = tag.getAttributeValue("id");
            if (!StringUtils.isEmpty(id)) {
                path = path + "[@id='" + id + "']";
            } else { //Get the from tag and extract its uri attribute
                XmlTag from = tag.findFirstSubTag("from");
                String fromUri = from.getAttributeValue("uri");
                path = path + "[from[attribute::uri = '" + fromUri + "']]";
            }
            return "/" + path;
        } else {
            XmlTag parent = tag.getParentTag();
            XmlTag[] siblings = parent.findSubTags(tag.getName());
            for (int i = 0; i < siblings.length; i++) {
                if (tag.equals(siblings[i])) {
                    path = path + "[" + String.valueOf(i + 1) + "]";
                    break;
                }
            }
            path = getXPathOfTheXMLTag(parent) + "/" + path;
        }
        return path;
    }

    private String getXPathOfTheElement(Element element) {
        String path = element.getTagName();
        if ("route".equals(path)) {
            //Get the from tag and extract its uri attribute
            Element from = (Element) element.getElementsByTagName("from").item(0);
            String fromUri = from.getAttribute("uri");
            path = path + "[from[attribute::uri = '" + fromUri + "']]";
            return "//" + path;
        } else {
            Element parent = (Element) element.getParentNode();
            int index = 0;
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node nextChild = children.item(i);
                if (nextChild instanceof Element && element.getTagName().equals(((Element) nextChild).getTagName())) {
                    index++;
                    if (element.equals((Element) nextChild)) {
                        path = path + "[" + String.valueOf(index) + "]";
                        break;
                    }
                }
            }

            path = getXPathOfTheElement(parent) + "/" + path;
        }
        return path;
    }

    private Element getParentRouteId(String id) throws Exception {
        String path = "//route[*[attribute::id = '" + id + "']]";
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node tagNode = (Node) xPath.compile(path).evaluate(routesDOMDocument, XPathConstants.NODE);
        if (tagNode == null) {
            return null;
        }
        Element tag = (Element) tagNode;
        return tag;
    }

    private List<CamelMessageInfo> getStack(String breakpointId, String suspendedMessage) throws Exception {
        List<CamelMessageInfo> stack = new ArrayList<>();

        String messageHistory = (String) serverConnection.invoke(this.debuggerMBeanObjectName, "evaluateExpressionAtBreakpoint",
                new Object[]{breakpointId, "simple", "${messageHistory(false)}", "java.lang.String"},
                new String[]{"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"});

        if (!StringUtils.isEmpty(messageHistory)) {
            String separator = System.getProperty("line.separator");
            String[] lines = messageHistory.split(separator);
            for (int i = 4; i < lines.length; i++) {
                String[] cols = lines[i].split("\\] \\[");
                String routeId = cols[0].substring(1).trim();
                String processorId = cols[1].trim();
                String processor = cols[2].trim();
                CamelBreakpoint breakpoint = breakpoints.get(processorId);
                if (breakpoint == null) {
                    //find tag and source position based on ID
                    breakpoint = getCamelBreakpointById(processorId);
                    breakpoints.put(processorId, breakpoint);
                }
                CamelMessageInfo info = new CamelMessageInfo(suspendedMessage, breakpoint.getXSourcePosition(), breakpoint.getBreakpointTag(), routeId, processorId, processor, null);
/*
                Map<String, String> stackEntry = new HashMap<>();
                stackEntry.put("routeId", cols[0].substring(1).trim());
                stackEntry.put("processorId", cols[1].trim());
                stackEntry.put("processor", cols[2].trim());
*/
                stack.add(info);
            }
        }

        Collections.reverse(stack);
        return stack;
    }

}
