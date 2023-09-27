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
import com.github.cameltooling.idea.service.CamelRuntime;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
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
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.github.cameltooling.idea.runner.debugger.CamelDebuggerContext.CAMEL;
import static com.github.cameltooling.idea.runner.debugger.CamelDebuggerTarget.BODY;
import static com.github.cameltooling.idea.runner.debugger.CamelDebuggerTarget.EXCHANGE_PROPERTY;
import static com.github.cameltooling.idea.runner.debugger.CamelDebuggerTarget.MESSAGE_HEADER;

public class CamelDebuggerSession implements AbstractDebuggerSession {
    private static final Logger LOG = Logger.getInstance(CamelDebuggerSession.class);

    private static final int MAX_RETRIES = 30;
    private static final String BACKLOG_DEBUGGER_LOGGING_LEVEL = "TRACE";
    private static final long FALLBACK_TIMEOUT = Long.MAX_VALUE - 1;
    private static final String MAIN_RESOURCES_RELATIVE_PATH = "src/main/resources/";
    /**
     * All breakpoints to add that are kept in memory to register them on connect or re-connect.
     */
    private final List<XLineBreakpoint<XBreakpointProperties<?>>> breakpointsAdd = new CopyOnWriteArrayList<>();
    /**
     * All breakpoints to remove that are kept in memory to register them on connect or re-connect.
     */
    private final List<XLineBreakpoint<XBreakpointProperties<?>>> breakpointsRemove = new CopyOnWriteArrayList<>();

    private final Map<String, CamelBreakpoint> breakpoints = new ConcurrentHashMap<>();
    private final List<String> explicitBreakpointIDs = new CopyOnWriteArrayList<>();

    private final List<MessageReceivedListener> messageReceivedListeners = new CopyOnWriteArrayList<>();

    private final Project project;

    private volatile ManagedBacklogDebuggerMBean backlogDebugger;
    private volatile JMXConnector connector;
    private volatile MBeanServerConnection serverConnection;
    private volatile ObjectName debuggerMBeanObjectName;

    private volatile org.w3c.dom.Document routesDOMDocument;

    private volatile String temporaryBreakpointId;

    private final XDebugSession xDebugSession;
    @Nullable
    private final ProcessHandler javaProcessHandler;
    private final String jmxHost;
    private final int jmxPort;

    public CamelDebuggerSession(Project project, XDebugSession session, @NotNull ProcessHandler javaProcessHandler) {
        this.project = project;
        this.xDebugSession = session;
        this.javaProcessHandler = javaProcessHandler;
        this.jmxHost = "localhost";
        this.jmxPort = 1099;
    }

    public CamelDebuggerSession(Project project, XDebugSession session, String jmxHost, int jmxPort) {
        this.project = project;
        this.xDebugSession = session;
        this.javaProcessHandler = null;
        this.jmxHost = jmxHost;
        this.jmxPort = jmxPort;
    }

    public String getJMXServiceURL() {
        return "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi/camel".formatted(jmxHost, jmxPort);
    }

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

    public void connect() {
        ApplicationManager.getApplication().executeOnPooledThread(this::checkSuspendedBreakpoints);
    }

    public void disconnect() {
        if (backlogDebugger != null) {
            try {
                backlogDebugger.detach();
            } catch (Exception e) {
                LOG.warn("Could not detach the debugger: " + e.getMessage());
            }
            try {
                backlogDebugger.disableDebugger();
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not disable the BacklogDebugger: " + e.getMessage());
                }
            } finally {
                backlogDebugger = null;
            }
        }
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not close the JMXConnector: " + e.getMessage());
                }
            } finally {
                serverConnection = null;
                connector = null;
            }
        }
    }

    public void addBreakpoint(XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint) {
        breakpointsAdd.add(xBreakpoint);
        breakpointsRemove.remove(xBreakpoint);
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, true);
        }
    }

    public void removeBreakpoint(XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint) {
        breakpointsAdd.remove(xBreakpoint);
        breakpointsRemove.add(xBreakpoint);
        if (isConnected()) {
            toggleBreakpoint(xBreakpoint, false);
        }
    }

    public Project getProject() {
        return project;
    }

    public void addMessageReceivedListener(MessageReceivedListener listener) {
        messageReceivedListeners.add(listener);
    }

    public void resume() {
        if (isConnected()) {
            //Remove temporary breakpoint
            if (temporaryBreakpointId != null && !explicitBreakpointIDs.contains(temporaryBreakpointId)) {
                backlogDebugger.removeBreakpoint(temporaryBreakpointId);
            }
            backlogDebugger.resumeAll();
        }
        temporaryBreakpointId = null;
    }

    public void setValue(CamelDebuggerTarget target,
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
                LOG.debug(String.format("The breakpoint element could not be created from the position %s", position));
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
                if (target == MESSAGE_HEADER) {
                    serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageHeaderOnBreakpoint",
                            new Object[]{breakpointId, targetName, value},
                            new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});
                } else if (target == EXCHANGE_PROPERTY) {
                    serverConnection.invoke(this.debuggerMBeanObjectName, "setExchangePropertyOnBreakpoint",
                            new Object[]{breakpointId, targetName, value},
                            new String[]{"java.lang.String", "java.lang.String", "java.lang.Object"});
                } else if (target == BODY) {
                    serverConnection.invoke(this.debuggerMBeanObjectName, "setMessageBodyOnBreakpoint",
                            new Object[]{breakpointId, value},
                            new String[]{"java.lang.String", "java.lang.Object"});
                }
            }
        } catch (Exception e) {
            LOG.warn(String.format("Could not evaluate the expression %s", expression), e);
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
            List<CamelMessageInfo> stack = getStack(breakpointId, dumpTracedMessagesAsXml(breakpointId));
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

    public CamelDebugProcess getCamelDebugProcess() {
        ContextAwareDebugProcess debugProcess = (ContextAwareDebugProcess) xDebugSession.getDebugProcess();
        return debugProcess.getDebugProcess(CAMEL, CamelDebugProcess.class);
    }

    /**
     * Tries to connect to the JMX connector server. In case of failure, it retries every 2 seconds several times, and if
     * after {@link #MAX_RETRIES}, it still cannot connect, it stops trying.
     *
     * @return {@code true} if it could connect to the JMX connector server, {@code false} otherwise.
     */
    private boolean tryToConnect() {
        for (int i = 0; i < MAX_RETRIES && canConnect(); i++) {
            LOG.debug("Trying to connect to the JMX connector server");
            if (doConnect()) {
                LOG.debug("Connected with success to the JMX connector server");
                return true;
            }
            try {
                Thread.sleep(1000L * 2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    private boolean canConnect() {
        if (javaProcessHandler == null) {
            return !xDebugSession.isStopped();
        }
        return !javaProcessHandler.isProcessTerminated() && !javaProcessHandler.isProcessTerminating();
    }

    private boolean doConnect() {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ClasspathUtils.getProjectClassLoader(project, this.getClass().getClassLoader()));

            this.connector = getJMXConnector();
            if (connector == null) {
                return false;
            } else {
                this.serverConnection = connector.getMBeanServerConnection();
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
                    String routes = camelContext.dumpRoutesAsXml(false);
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                    InputStream targetStream = new ByteArrayInputStream(routes.getBytes());
                    this.routesDOMDocument = documentBuilder.parse(targetStream);

                    //TODO get list of loaded expression languages

                }

                //Toggle all pending breakpoints
                for (XLineBreakpoint<XBreakpointProperties<?>> breakpoint : breakpointsRemove) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            toggleBreakpoint(breakpoint, false);
                        } catch (Exception e) {
                            LOG.error(e);
                        }
                    });
                }
                for (XLineBreakpoint<XBreakpointProperties<?>> breakpoint : breakpointsAdd) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            toggleBreakpoint(breakpoint, true);
                        } catch (Exception e) {
                            LOG.error(e);
                        }
                    });
                }
                try {
                    backlogDebugger.attach();
                } catch (Exception e) {
                    LOG.warn("Could not attach the debugger: " + e.getMessage());
                }
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

    /**
     * @return the {@link JMXConnector} corresponding to the Java process. In case the process id
     * cannot be found, it calls {@link #getJMXConnectorFromServiceURL()} as fallback.
     */
    private JMXConnector getJMXConnectorFromLocalJavaProcess() {
        final String javaProcessPID = getPID(javaProcessHandler);
        if (javaProcessPID == null) {
            return getJMXConnectorFromServiceURL();
        }
        try {
            final VirtualMachine vm = VirtualMachine.attach(javaProcessPID);
            vm.startLocalManagementAgent();
            final String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            vm.detach();
            return JMXConnectorFactory.connect(new JMXServiceURL(connectorAddress));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not retrieve the JMX API connector of the java process " + javaProcessPID, e);
            }
        }
        return null;
    }

    /**
     * @return the {@link JMXConnector} that matches the best with the current context.
     */
    private JMXConnector getJMXConnector() {
        if (CamelRuntime.getCamelRuntime(project) == CamelRuntime.QUARKUS) {
            // In case of Quarkus, the application runs in a forked process such that the JMXConnector needs to be
            // retrieved from a URL corresponding to a remote process
            return getJMXConnectorFromServiceURL();
        }
        return getJMXConnectorFromLocalJavaProcess();
    }

    @Nullable
    private JMXConnector getJMXConnectorFromServiceURL() {
        try {
            return JMXConnectorFactory.connect(new JMXServiceURL(getJMXServiceURL()));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not retrieve the JMX API connector", e);
            }
        }
        return null;
    }

    private static String getPID(ProcessHandler handler) {
        if (handler == null) {
            return null;
        }
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
        while (isConnected() || tryToConnect()) {
            try {
                LOG.debug("Collecting suspended breakpoint nodes ids");
                @SuppressWarnings("unchecked")
                Collection<String> suspendedBreakpointIDs = (Collection<String>) serverConnection.invoke(
                    this.debuggerMBeanObjectName, "suspendedBreakpointNodeIds", new Object[]{}, new String[]{}
                );
                if (suspendedBreakpointIDs != null && !suspendedBreakpointIDs.isEmpty()) {
                    LOG.debug("Found suspended breakpoint nodes ids: ", suspendedBreakpointIDs.size());
                    //Fire notifications here, we need to display the exchange, stack etc
                    ApplicationManager.getApplication().runReadAction(() -> {
                        final List<CamelMessageInfo> messages = suspendedBreakpointIDs.stream()
                            .map(this::getCamelMessageInfo)
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparing(CamelMessageInfo::getTimestamp))
                            .collect(Collectors.toList());
                        if (messages.isEmpty()) {
                            LOG.debug("No message info could be collected");
                            return;
                        }
                        notifyMessageReceivedListeners(messages);
                    });
                }
                Thread.sleep(1_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.warn("Could not check suspended breakpoints", e);
            }
        }
        LOG.debug("Stop collecting suspended breakpoint nodes ids");
    }

    /**
     * Notifies all the {@link MessageReceivedListener} that new messages have been received.
     * @param camelMessages the info of the messages to provide to the listeners.
     */
    private void notifyMessageReceivedListeners(List<CamelMessageInfo> camelMessages) {
        LOG.debug("Notifying the message listeners");
        for (MessageReceivedListener listener : messageReceivedListeners) {
            try {
                listener.onMessagesReceived(camelMessages);
            } catch (Exception e) {
                LOG.warn("Could not notify the message listener", e);
            }
        }
    }

    /**
     * Retrieves the {@link CamelMessageInfo} corresponding to the given id of breakpoint.
     * @param id the id of the breakpoint
     * @return the {@link CamelMessageInfo} corresponding to the given id of breakpoint if it could be found, {@code null}
     * otherwise.
     */
    @Nullable
    private CamelMessageInfo getCamelMessageInfo(String id) {
        String xml = null;
        try {
            CamelBreakpoint breakpoint = breakpoints.get(id);
            if (breakpoint == null) {
                //find tag and source position based on ID
                breakpoint = getCamelBreakpointById(id);
                breakpoints.put(id, breakpoint);
            }
            xml = dumpTracedMessagesAsXml(id);
            final List<CamelMessageInfo> stack = getStack(id, xml);
            final CamelMessageInfo info = stack.get(0); // We only need stack for the top frame
            info.setStack(stack);
            return info;
        } catch (IndexNotReadyException e) {
            DumbService.getInstance(project)
                .showDumbModeNotification(
                    String.format(
                        "The Camel Debugger is disabled while %s is updating indices", ApplicationNamesInfo.getInstance().getProductName()
                    )
                );
        } catch (Exception e) {
            LOG.warn("Could not collect the camel message info: " + e.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not collect the camel message info from id = " + id + " and xml = " + xml, e);
            }
        }
        return null;
    }

    /**
     * Invokes through JMX {@code dumpTracedMessagesAsXml} using the new method signature, if it fails, it retries using
     * the old signature.
     *
     * @param id The node id for which the method {@code dumpTracedMessagesAsXml} should be invoked.
     * @return the result of the method {@code dumpTracedMessagesAsXml} that could be called through JMX.
     */
    private String dumpTracedMessagesAsXml(String id) {
        String xml;
        try {
            // If the Camel version is 3.15 or later, the exchange properties are included
            xml = backlogDebugger.dumpTracedMessagesAsXml(id, true);
        } catch (Exception e) {
            LOG.warn("Could not invoke dumpTracedMessagesAsXml(" + id + ", true)", e);
            // Could not invoke the dumpTracedMessagesAsXml with the new signature let's try the old one
            try {
                xml = (String) serverConnection.invoke(this.debuggerMBeanObjectName, "dumpTracedMessagesAsXml", new Object[]{id},
                    new String[]{"java.lang.String"});
            } catch (Exception ex) {
                LOG.error("Could not invoke dumpTracedMessagesAsXml(" + id + ")", e);
                return "";
            }
        }
        return xml;
    }

    private Element getParentRouteId(String id) throws Exception {
        String path = "//route[*[attribute::id = '" + id + "']]";
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (Element) xPath.compile(path).evaluate(routesDOMDocument, XPathConstants.NODE);
    }

    private List<CamelMessageInfo> getStack(String breakpointId, String messageInfoAsXML) throws Exception {
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
                CamelMessageInfo info = new CamelMessageInfo(messageInfoAsXML, breakpoint.getXSourcePosition(), breakpoint.getBreakpointTag(), routeId, processorId, processor, null);
                stack.add(info);
            }
        }

        Collections.reverse(stack);
        return stack;
    }

    private String getBreakpointId(@NotNull PsiElement breakpointTag) {
        String breakpointId = null;
        final List<String> sourceLocations;

        //Obtain file name and line number
        XSourcePosition position = XDebuggerUtil.getInstance().createPositionByElement(breakpointTag);
        int lineNumber = position.getLine() + 1; //Lines in XSourcePosition are 0-based

        final VirtualFile virtualFile = position.getFile();

        switch (virtualFile.getFileType().getName()) {
        case "XML":
        case "YAML":
            final String url = virtualFile.getPresentableUrl();
            if (virtualFile.isInLocalFileSystem()) { //TODO - we need a better way to match source to target
                /*
                In Camel Quarkus, the response form camelContext.dumpRoutesAsXml() has sourceLocation attribute values
                in form of classpath:myroutesfile.xml or file:src/main/resources/myroutesfile.xml as opposed to
                file:/com/foo/bar/target/classes/myroutesfile.xml.
                So we need to add both to the list of source locations. See issue #820.
                 */
                sourceLocations = new ArrayList<>();
                String sourcesPath = "src/main/resources";
                if (url.contains(sourcesPath)) {
                    // file:/absolute/path/to/file.xml
                    sourceLocations.add(String.format("file:%s", url.replace(sourcesPath, "target/classes"))); // maven
                    sourceLocations.add(String.format("file:%s", url.replace(sourcesPath, "build/resources/main"))); // gradle
                }
                int index = url.lastIndexOf(MAIN_RESOURCES_RELATIVE_PATH);
                if (index != -1) {
                    sourceLocations.add(String.format("file:%s", url.substring(index))); // file:/relative/path/to/file.xml
                    sourceLocations.add(String.format("classpath:%s", url.substring(index + MAIN_RESOURCES_RELATIVE_PATH.length())));
                }
                String basePath = getProject().getBasePath();
                if (basePath != null && url.startsWith(basePath)) {
                    sourceLocations.add(String.format("file:%s", url.substring(basePath.length() + 1))); // file:file.xml
                }
            } else { //Then it must be a Jar
                sourceLocations = List.of(String.format("classpath:%s", url.substring(url.lastIndexOf("!") + 2)));
            }
            break;
        case "JAVA":
            PsiClass psiClass = PsiTreeUtil.getParentOfType(breakpointTag, PsiClass.class);
            sourceLocations = List.of(psiClass.getQualifiedName(), virtualFile.getName());
            break;
        default: // noop
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("File type not supported: %s", virtualFile.getFileType().getName()));
            }
            return null;
        }

        String path = sourceLocations
            .stream()
            .map(sourceLocation -> String.format("//*[@sourceLocation='%s' and @sourceLineNumber='%d']", sourceLocation, lineNumber))
            .collect(Collectors.joining("|"));

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
            final VirtualFile virtualFile;
            if (fileName.endsWith(".java")) {
                Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName(
                    fileName, GlobalSearchScope.everythingScope(project)
                );
                if (virtualFiles.isEmpty()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("The file %s could not be found in the project", fileName));
                    }
                    return null;
                }
                virtualFile = virtualFiles.iterator().next();
            } else {
                PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fileName, GlobalSearchScope.everythingScope(project));
                virtualFile = psiClass.getContainingFile().getVirtualFile();
            }
            XSourcePosition position = XDebuggerUtil.getInstance().createPosition(virtualFile, Integer.valueOf(lineNumber) - 1);
            Map<String, PsiElement> breakpointElement = createBreakpointElementFromPosition(position);
            return new CamelBreakpoint(id, breakpointElement.get(id), position);
        } else {
            Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName(
                fileName, GlobalSearchScope.everythingScope(project)
            );
            if (virtualFiles.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("The file %s could not be found in the project", fileName));
                }
                return null;
            }
            for (VirtualFile virtualFile : virtualFiles) {
                Set<String> potentialURLs;
                String url = virtualFile.getPresentableUrl();
                if (virtualFile.isInLocalFileSystem()) { //TODO - we need a better way to match source to target
                    if (filePath.startsWith("classpath:")) {
                        int index = url.lastIndexOf(MAIN_RESOURCES_RELATIVE_PATH);
                        if (index != -1) {
                            //classpath:relative/path/from/resources/file.xml
                            potentialURLs = Set.of(String.format("classpath:%s", url.substring(index + MAIN_RESOURCES_RELATIVE_PATH.length())));
                        } else {
                            potentialURLs = Set.of();
                        }
                    } else {
                        potentialURLs = new HashSet<>();
                        String sourcesPath = "src/main/resources";
                        if (url.contains(sourcesPath)) {
                            // file:/absolute/path/to/file.xml
                            potentialURLs.add(String.format("file:%s", url.replace(sourcesPath, "target/classes"))); // maven
                            potentialURLs.add(String.format("file:%s", url.replace(sourcesPath, "build/resources/main"))); // gradle
                        }
                        int index = url.lastIndexOf(MAIN_RESOURCES_RELATIVE_PATH);
                        if (index != -1) {
                            potentialURLs.add(String.format("file:%s", url.substring(index))); // file:/relative/path/to/file.xml
                        }
                        String basePath = getProject().getBasePath();
                        if (basePath != null && url.startsWith(basePath)) {
                            potentialURLs.add(String.format("file:%s", url.substring(basePath.length() + 1))); // file:file.xml
                        }
                    }
                } else { //Then it must be a Jar
                    potentialURLs = Set.of(String.format("classpath:%s", url.substring(url.lastIndexOf("!") + 2)));
                }
                if (potentialURLs.contains(filePath)) {
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
        if (position == null) {
            return null;
        }
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
