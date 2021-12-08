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

import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.AbstractDebuggerSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.sun.tools.attach.VirtualMachine;
import org.apache.camel.api.management.mbean.ManagedBacklogDebuggerMBean;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CamelDebuggerSession implements AbstractDebuggerSession {

    private static final int MAX_RETRIES = 30;

    private final List<XLineBreakpoint<XBreakpointProperties>> addBreakpoints = new ArrayList<>();
    private final List<XLineBreakpoint<XBreakpointProperties>> removeBreakpoints = new ArrayList<>();

    private final Map<String, XLineBreakpoint<XBreakpointProperties>> breakpoints = new HashMap<>();
    private final List<String> suspendedBreakpoints = new ArrayList<>();

    private final List<MessageReceivedListener> messageReceivedListeners = new ArrayList<>();

    private Project project;
    private ManagedBacklogDebuggerMBean backlogDebugger;
    private ManagedCamelContextMBean camelContext;

    private MBeanServerConnection serverConnection;
    private ObjectName debuggerMBeanObjectName;

    private org.w3c.dom.Document routesDOMDocument;

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
        backlogDebugger.disableDebugger();
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
            }
        }
    }

    public void addBreakpoint(XLineBreakpoint<XBreakpointProperties> xBreakpoint) {
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, true);
        } else {
            addBreakpoints.add(xBreakpoint);
            removeBreakpoints.remove(xBreakpoint);
        }
    }

    public void removeBreakpoint(XLineBreakpoint<XBreakpointProperties> xBreakpoint) {
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, false);
        } else {
            addBreakpoints.remove(xBreakpoint);
            removeBreakpoints.add(xBreakpoint);
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
        for (String suspendedId : suspendedBreakpoints) {
            backlogDebugger.resumeBreakpoint(suspendedId);
        }
        suspendedBreakpoints.clear();
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

        try {
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

                //Lookup camel context
                objectName = new ObjectName("org.apache.camel:context=*,type=context,name=*");
                names = serverConnection.queryNames(objectName, null);
                if (names != null && !names.isEmpty()) {
                    ObjectName mbeanName = names.iterator().next();
                    camelContext = JMX.newMBeanProxy(serverConnection, mbeanName, ManagedCamelContextMBean.class);

                    //Init DOM Document
                    String routes = getCamelContext().dumpRoutesAsXml(true, true);
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                    InputStream targetStream = new ByteArrayInputStream(routes.getBytes());
                    this.routesDOMDocument = documentBuilder.parse(targetStream);
                }

                //Toggle all pending breakpoints
                for (XLineBreakpoint breakpoint : addBreakpoints) {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        ApplicationManager.getApplication().runReadAction(() -> {
                            toggleBreakpoint(breakpoint, true);
                        });
                    });
                }
                for (XLineBreakpoint breakpoint : removeBreakpoints) {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        ApplicationManager.getApplication().runReadAction(() -> {
                            toggleBreakpoint(breakpoint, false);
                        });
                    });
                }
                addBreakpoints.clear();
                removeBreakpoints.clear();

                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
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

    private boolean toggleBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> xBreakpoint, boolean toggleOn) {
        String breakpointId = null;
        XSourcePosition position = xBreakpoint.getSourcePosition();

        XmlTag breakpointTag = getXmlTagAt(project, position);
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
            return false;
        }

        //TODO Conditions

        if (breakpointId != null) {
            if (toggleOn) {
                backlogDebugger.addBreakpoint(breakpointId);
            } else {
                backlogDebugger.removeBreakpoint(breakpointId);
            }

            breakpoints.put(breakpointId, xBreakpoint);

            return true;
        }

        return false;
    }

    private void checkSuspendedBreakpoints() {
        while (isConnected()) {
            try {
                //Set<String> suspendedBreakpointIDs = backlogDebugger.getSuspendedBreakpointNodeIds();
                // this throws exception: javax.management.AttributeNotFoundException: getAttribute failed: ModelMBeanAttributeInfo not found for SuspendedBreakpointNodeIds
                //	at java.management/javax.management.modelmbean.RequiredModelMBean.getAttribute(RequiredModelMBean.java:1440)
                //	at java.management/com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.getAttribute(DefaultMBeanServerInterceptor.java:641)
                //	at java.management/com.sun.jmx.mbeanserver.JmxMBeanServer.getAttribute(JmxMBeanServer.java:678)
                //	at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl.doOperation(RMIConnectionImpl.java:1443)
                //	at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl$PrivilegedOperation.run(RMIConnectionImpl.java:1307)
                //	at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl.doPrivilegedOperation(RMIConnectionImpl.java:1399)
                //	at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl.getAttribute(RMIConnectionImpl.java:637)
                //	at java.base/jdk.internal.reflect.GeneratedMethodAccessor151.invoke(Unknown Source)
                //	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                //	at java.base/java.lang.reflect.Method.invoke(Method.java:567)
                //	at java.rmi/sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:359)
                //	at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:200)
                //	at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:197)
                //	at java.base/java.security.AccessController.doPrivileged(AccessController.java:689)
                //	at java.rmi/sun.rmi.transport.Transport.serviceCall(Transport.java:196)
                //	at java.rmi/sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:562)
                //	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:796)
                //	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:677)
                //	at java.base/java.security.AccessController.doPrivileged(AccessController.java:389)
                //	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:676)
                //	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
                //	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
                //	at java.base/java.lang.Thread.run(Thread.java:835)
                //	at java.rmi/sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:303)
                //	at java.rmi/sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:279)
                //	at java.rmi/sun.rmi.server.UnicastRef.invoke(UnicastRef.java:164)
                //	at jdk.remoteref/jdk.jmx.remote.internal.rmi.PRef.invoke(Unknown Source)
                //	at java.management.rmi/javax.management.remote.rmi.RMIConnectionImpl_Stub.getAttribute(Unknown Source)
                //	at java.management.rmi/javax.management.remote.rmi.RMIConnector$RemoteMBeanServerConnection.getAttribute(RMIConnector.java:904)
                //	at java.management/javax.management.MBeanServerInvocationHandler.invoke(MBeanServerInvocationHandler.java:273)
                //	... 13 more

                Collection<String> suspendedBreakpointIDs = (Collection<String>) serverConnection.invoke(this.debuggerMBeanObjectName, "getSuspendedBreakpointNodeIds", new Object[] { } , new String[] { });
                if (suspendedBreakpointIDs != null && !suspendedBreakpointIDs.isEmpty()) {
                    //Fire notifications here, we need to display the exchange, stack etc
                    for (String id : suspendedBreakpointIDs) {
                        if (!suspendedBreakpoints.contains(id)) {
                            String suspendedMessage = backlogDebugger.dumpTracedMessagesAsXml(id);
                            suspendedBreakpoints.add(id);
                            ApplicationManager.getApplication().runReadAction(() -> {
                                for (MessageReceivedListener listener : messageReceivedListeners) {
                                    try {
                                        XLineBreakpoint<XBreakpointProperties> xLineBreakpoint = breakpoints.get(id);
                                        XSourcePosition position = xLineBreakpoint.getSourcePosition();
                                        XmlTag breakpointTag = getXmlTagAt(project, position);
                                        listener.onNewMessageReceived(new CamelMessageInfo(suspendedMessage, xLineBreakpoint, breakpointTag));
                                    } catch (Exception e) {

                                    }
                                }
                            });
                        }
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

    //================= Private XML helper methods
    @Nullable
    private XmlTag getXmlTagAt(Project project, XSourcePosition sourcePosition) {
        final VirtualFile file = sourcePosition.getFile();
        final XmlFile xmlFile = (XmlFile) PsiManager.getInstance(project).findFile(file);
        final XmlTag rootTag = xmlFile.getRootTag();
        return findXmlTag(sourcePosition, rootTag);
    }

    private XmlTag findXmlTag(XSourcePosition sourcePosition, XmlTag rootTag) {
        final XmlTag[] subTags = rootTag.getSubTags();
        for (int i = 0; i < subTags.length; i++) {
            XmlTag subTag = subTags[i];
            final int subTagLineNumber = getLineNumber(sourcePosition.getFile(), subTag);
            if (subTagLineNumber == sourcePosition.getLine()) {
                return subTag;
            } else if (subTagLineNumber > sourcePosition.getLine() && i > 0 && subTags[i - 1].getSubTags().length > 0) {
                return findXmlTag(sourcePosition, subTags[i - 1]);
            }
        }
        if (subTags.length > 0) {
            final XmlTag lastElement = subTags[subTags.length - 1];
            return findXmlTag(sourcePosition, lastElement);
        } else {
            return null;
        }
    }

    private int getLineNumber(VirtualFile file, XmlTag tag) {
        final int offset = tag.getTextOffset();
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        return offset < document.getTextLength() ? document.getLineNumber(offset) : -1;
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
}
