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
package com.github.cameltooling.idea.service.extension.camel;

import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.JavaClassUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(JavaCamelIdeaUtils.class);
    private static final String JAVA_LANG_STRING = "java.lang.String";
    /**
     * The pattern corresponding to a method call in Java.
     */
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("(?s)(\\s*\\)\\s*\\))?(\\s*)(\\.)?(\\s*)(\\w+)(\\s*)\\(");
    /**
     * Name of the methods indicating that the next method call needs to be indented one more time.
     */
    private static final Set<String> ADD_INDENT = Set.of("choice", "doTry", "pipeline", "multicast", "split",
        "circuitBreaker", "intercept", "interceptFrom", "interceptSendToEndpoint", "aggregate", "loadBalance", "loop",
        "kamelet", "step", "transacted", "saga", "route", "resequence", "policy", "onException", "onCompletion",
        "from", "rest", "restConfiguration");
    /**
     * Name of the methods corresponding to the root element of sub DSL.
     */
    private static final Set<String> SUB_DSL_ROOTS = Set.of("expression", "dataFormat");
    /**
     * Name of the methods indicating that the next method call needs to be indented the same way as before but the
     * method call itself must be indented one less time.
     */
    private static final Set<String> NEW_BLOCK = Set.of("otherwise", "doCatch", "doFinally", "onFallback");
    /**
     * Name of the methods indicating that the expected behavior is either {@link #ADD_INDENT} or {@link #NEW_BLOCK}
     * according to last method name. If it is the same name as the last name, {@link #NEW_BLOCK} is expected, {@link #ADD_INDENT}
     * otherwise.
     */
    private static final Set<String> ADD_INDENT_OR_NEW_BLOCK = Set.of("when");
    /**
     * Name of the methods indicating that the next method call needs to be indented one less time.
     */
    private static final Set<String> REMOVE_INDENT = Set.of("endChoice", "end", "endParent", "endDoTry", "endDoCatch",
        "endCircuitBreaker");
    private static final List<String> JAVA_ROUTE_BUILDERS = Arrays.asList(
        "org.apache.camel.builder.RouteBuilder",
        "org.apache.camel.RoutesBuilder",
        "org.apache.camel.builder.RouteConfigurationBuilder",
        "org.apache.camel.RouteConfigurationsBuilder",
        "org.apache.camel.builder.AdviceWithRouteBuilder",
        "org.apache.camel.spring.SpringRouteBuilder",
        "org.apache.camel.builder.endpoint.EndpointRouteBuilder"
    );
    private static final List<String> BEAN_ANNOTATIONS = Arrays.asList(
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Repository",
        "javax.inject.Named",
        "javax.inject.Singleton",
        "javax.enterprise.context.ApplicationScoped",
        "javax.enterprise.context.SessionScoped",
        "javax.enterprise.context.ConversationScoped",
        "javax.enterprise.context.RequestScoped",
        "jakarta.inject.Named",
        "jakarta.inject.Singleton",
        "jakarta.enterprise.context.ApplicationScoped",
        "jakarta.enterprise.context.SessionScoped",
        "jakarta.enterprise.context.ConversationScoped",
        "jakarta.enterprise.context.RequestScoped"
    );

    @Override
    public boolean isCamelFile(PsiFile file) {
        if (file != null && JavaFileType.INSTANCE.equals(file.getFileType())
            && file instanceof PsiJavaFile javaFile) {
            final PsiClass[] classes = javaFile.getClasses();
            for (PsiClass nextClass : classes) {
                for (String nextBaseName : JAVA_ROUTE_BUILDERS) {
                    if (InheritanceUtil.isInheritor(nextClass, nextBaseName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        return IdeaUtils.getService().isFromJavaMethodCall(element, true, ROUTE_START);
    }

    @Override
    public PsiElement getLeafElementForLineMarker(PsiElement element) {
        if (element instanceof PsiLiteralExpression ple) {
            return ple.getFirstChild();
        }
        return null;
    }

    @Override
    public boolean isCamelRouteStartExpression(PsiElement element) {
        PsiElement routeStartParent = IdeaUtils.getService().findFirstParent(element, false,
            this::isCamelRouteStart, PsiFile.class::isInstance);
        return routeStartParent != null;
    }

    @Override
    public boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart) {
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call == null) {
            return false;
        }
        final IdeaUtils ideaUtils = IdeaUtils.getService();
        if (!excludeRouteStart && ideaUtils.isFromJavaMethod(call, true, ROUTE_START)) {
            return true;
        }
        Collection<PsiMethodCallExpression> chainedCalls = PsiTreeUtil.findChildrenOfType(call, PsiMethodCallExpression.class);
        return chainedCalls.stream().anyMatch(c -> ideaUtils.isFromJavaMethod(c, true, ROUTE_START));
    }

    @Override
    public boolean isCamelLineMarker(PsiElement element) {
        if (isPlaceForEndpointUri(element)) {
            return true;
        }

        if (element instanceof PsiIdentifier identifier) {
            // check for the pattern "rest()"
            //TODO: find out why do we want to place line marker on rest DSL, what's the use case?
            if ("rest".equals(identifier.getText())) {
                PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
                return call != null && call.getArgumentList().isEmpty();
            }

            // check for a method call inside a start of a camel route (e.g. inside of a from(...))
            PsiMethodCallExpression methodCall = PsiTreeUtil.getParentOfType(element, true, PsiMethodCallExpression.class);
            if (methodCall != null) {
                 boolean deepest = isDeepestMethodCall(methodCall); // choose only deepest method call of a call chain
                 if (deepest && isCamelRouteStartExpression(methodCall)) {
                     return true;
                 }
            }

            // check for a PsiReferenceExpression (e.g. a variable, constant, ...) inside a method call, that's a start of a camel route
            PsiReferenceExpression reference = PsiTreeUtil.getParentOfType(element, true, PsiReferenceExpression.class);
            if (reference != null) {
                boolean methodName = reference.getParent() instanceof PsiMethodCallExpression; //let's exclude the method name itself
                if (!methodName && isCamelRouteStartExpression(reference)) {
                    return true;
                }
            }

        }

        return false;
    }

    private boolean isDeepestMethodCall(PsiMethodCallExpression methodCall) {
        return PsiTreeUtil.findChildrenOfType(methodCall, PsiMethodCallExpression.class).isEmpty();
    }

    @Override
    public boolean isCamelExpression(PsiElement element, String language) {
        // java method call
        String[] methods = null;
        if ("simple".equals(language)) {
            methods = new String[]{"simple", "log"};
        } else if ("jsonpath".equals(language)) {
            methods = new String[]{"jsonpath"};
        }
        return IdeaUtils.getService().isFromJavaMethodCall(element, true, methods);
    }

    @Override
    public boolean isCamelExpressionUsedAsPredicate(PsiElement element, String language) {
        // java
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {

            if ("simple".equals(language)) {
                // extra check for simple language
                PsiMethod method = call.resolveMethod();
                if (method != null) {
                    // if its coming from the log EIP then its not a predicate
                    String name = method.getName();
                    if ("log".equals(name)) {
                        return false;
                    }
                }
            }

            // okay dive into the psi and find out which EIP are using the simple
            PsiElement child = call.getFirstChild();
            if (child instanceof PsiReferenceExpression psiReferenceExpression) {
                // this code is needed as it may be used as a method call as a parameter and this requires
                // a bit of psi code to unwrap the right elements.
                PsiExpression exp = psiReferenceExpression.getQualifierExpression();
                if (exp == null) {
                    // okay it was not a direct method call, so see if it was passed in as a parameter instead (expression list)
                    element = element.getParent();
                    if (element instanceof PsiExpressionList) {
                        element = element.getParent();
                    }
                    if (element instanceof PsiMethodCallExpression) {
                        exp = PsiTreeUtil.getParentOfType(element.getParent(), PsiMethodCallExpression.class);
                    }
                }
                if (exp instanceof PsiMethodCallExpression psiMethodCallExpression) {
                    PsiMethod method = psiMethodCallExpression.resolveMethod();
                    if (method != null) {
                        String name = method.getName();
                        return Arrays.asList(PREDICATE_EIPS).contains(name);
                    }
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean isConsumerEndpoint(PsiElement element) {
        if (IdeaUtils.getService().isFromJavaMethodCall(element, true, CONSUMER_ENDPOINT)) {
            return true;
        }
        // annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Consume");
        }
        return false;
    }

    @Override
    public boolean isProducerEndpoint(PsiElement element) {
        if (IdeaUtils.getService().isFromJavaMethodCall(element, true, PRODUCER_ENDPOINT)) {
            return true;
        }
        // annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Produce");
        }
        return false;
    }

    @Override
    public boolean skipEndpointValidation(PsiElement element) {
        final IdeaUtils ideaUtils = IdeaUtils.getService();
        if (ideaUtils.isElementFromSetterProperty(element, "brokerURL")) {
            return true;
        }
        if (ideaUtils.isElementFromConstructor(element, "ActiveMQConnectionFactory")) {
            return true;
        }
        if (ideaUtils.isElementFromConstructor(element, "ActiveMQXAConnectionFactory")) {
            return true;
        }
        if (ideaUtils.isElementFromConstructor(element, "JmsConnectionFactory")) {
            return true;
        }
        if (ideaUtils.isElementFromAnnotation(element, "org.apache.camel.spi.UriEndpoint")) {
            return true;
        }
        return ideaUtils.isFromJavaMethodCall(element, false, "activeMQComponent");
    }

    @Override
    public boolean isFromStringFormatEndpoint(PsiElement element) {
        return IdeaUtils.getService().isFromJavaMethodCall(element, false, STRING_FORMAT_ENDPOINT);
    }

    @Override
    public boolean acceptForAnnotatorOrInspection(PsiElement element) {
        // skip XML limit on siblings
        if (!IdeaUtils.getService().isFromFileType(element, "xml")) {
            // for programming languages you can have complex structures with concat which we don't support it yet.
            // we currently only support oneliner, so check how many siblings the element has (it has 1 with ending parenthesis which is okay)
            return countSiblings(element) <= 1;
        }
        return true;
    }

    @Override
    public PsiClass getBeanClass(PsiElement element) {
        final PsiElement beanPsiElement = getPsiElementForCamelBeanMethod(element);
        if (beanPsiElement != null) {
            if (beanPsiElement instanceof PsiClass psiClass) {
                return psiClass;
            }

            PsiJavaCodeReferenceElement referenceElement = PsiTreeUtil.findChildOfType(beanPsiElement, PsiJavaCodeReferenceElement.class);
            final PsiClass psiClass = JavaClassUtils.getService().resolveClassReference(referenceElement);

            if (psiClass != null && !JAVA_LANG_STRING.equals(psiClass.getQualifiedName())) {
                return psiClass;
            }

            String beanName = "";
            if (referenceElement instanceof PsiReferenceExpression) {
                beanName = getStaticBeanName(referenceElement, beanName);
            } else {
                final String[] beanParameters = beanPsiElement.getText().replace("(", "").replace(")", "").split(",");
                if (beanParameters.length > 0) {
                    beanName = StringUtils.stripDoubleQuotes(beanParameters[0]);
                }
            }
            return searchForMatchingBeanClass(beanName, beanPsiElement.getProject()).orElse(null);
        }
        return null;
    }

    @Override
    public PsiElement getPsiElementForCamelBeanMethod(PsiElement element) {
        if (element instanceof PsiLiteral || element.getParent() instanceof PsiLiteralExpression) {
            final PsiExpressionList expressionList = PsiTreeUtil.getParentOfType(element, PsiExpressionList.class);
            if (expressionList != null) {
                final PsiIdentifier identifier = PsiTreeUtil.getChildOfType(expressionList.getPrevSibling(), PsiIdentifier.class);
                if (identifier != null && identifier.getNextSibling() == null && ("method".equals(identifier.getText()) || "bean".equals(identifier.getText()))) {
                    return expressionList;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isExtensionEnabled() {
        return true;
    }

    @Override
    public List<PsiElement> findEndpointUsages(Module module, Predicate<String> uriCondition) {
        return findEndpoints(module, uriCondition, e -> !isCamelRouteStart(e));
    }

    @Override
    public List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition) {
        return findEndpoints(module, uriCondition, this::isCamelRouteStart);
    }

    @Override
    public boolean isPlaceForEndpointUri(PsiElement location) {
        return location instanceof PsiLiteralExpression && isInsideCamelRoute(location, false);
    }

    @Override
    public List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderLocations() {
        return List.of(
                PsiJavaPatterns.literalExpression()
        );
    }

    /**
     * @return the {@link PsiClass} for the matching bean name by looking for classes annotated with
     * Spring Component, Service or Repository or Quarkus javax or jakarta annotations.
     */
    private Optional<PsiClass> searchForMatchingBeanClass(String beanName, Project project) {
        final JavaClassUtils javaClassUtils = JavaClassUtils.getService();

        return BEAN_ANNOTATIONS
            .stream()
            .map(annotation -> javaClassUtils.findBeanClassByName(beanName, annotation, project))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private List<PsiElement> findEndpoints(Module module, Predicate<String> uriCondition, Predicate<PsiLiteral> elementCondition) {
        PsiManager manager = PsiManager.getInstance(module.getProject());
        PsiClass routeBuilderClass = findRouteBuilderClass(manager);

        List<PsiElement> results = new ArrayList<>();
        if (routeBuilderClass != null) {
            Collection<PsiClass> routeBuilders = ClassInheritorsSearch.search(routeBuilderClass, module.getModuleScope(), true)
                .findAll();
            for (PsiClass routeBuilder : routeBuilders) {
                Collection<PsiLiteralExpression> literals = PsiTreeUtil.findChildrenOfType(routeBuilder, PsiLiteralExpression.class);
                for (PsiLiteralExpression literal : literals) {
                    Object val = literal.getValue();
                    if (val instanceof String endpointUri) {
                        if (uriCondition.test(endpointUri) && elementCondition.test(literal)) {
                            results.add(literal);
                        }
                    }
                }
            }
        }
        return results;
    }

    private PsiClass findRouteBuilderClass(PsiManager manager) {
        return JAVA_ROUTE_BUILDERS
                .stream()
                .map(fqn -> ClassUtil.findPsiClass(manager, fqn))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String getStaticBeanName(PsiJavaCodeReferenceElement referenceElement, String beanName) {
        final PsiType type = ((PsiReferenceExpression) referenceElement).getType();
        if (type != null && JAVA_LANG_STRING.equals(type.getCanonicalText())) {
            beanName = StringUtils.stripDoubleQuotes(PsiTreeUtil.getChildOfAnyType(referenceElement.getReference().resolve(), PsiLiteralExpression.class).getText());
        }
        return beanName;
    }

    @Override
    public TextRange processText(PsiFile source, TextRange rangeToReformat, CodeStyleSettings settings) {
        Document document = PsiDocumentManager.getInstance(source.getProject()).getDocument(source);
        if (document != null && !PostprocessReformattingAspect.getInstance(source.getProject()).isViewProviderLocked(source.getViewProvider())) {
            format(source, document, rangeToReformat.getStartOffset(), rangeToReformat.getEndOffset(), settings);
        }
        return rangeToReformat;
    }

    /**
     * Formats the routes that could be found between the given indexes.
     *
     * @param file        the file that contains the text to format.
     * @param document    the document that contains the text to format.
     * @param startOffset the start offset of the text format
     * @param endOffset   the end offset of the text format
     * @param settings    the settings to apply when formatting the text.
     */
    private void format(PsiFile file, Document document, int startOffset, int endOffset, CodeStyleSettings settings) {
        final VirtualFile vFile = FileDocumentManager.getInstance().getFile(document);
        if ((vFile == null || vFile instanceof LightVirtualFile) && !ApplicationManager.getApplication().isUnitTestMode()) {
            // we assume that control flow reaches this place when the document is backed by a "virtual" file so any changes made by
            // a formatter affect only PSI, and it is out of sync with a document text
            return;
        }

        try {
            ApplicationManager.getApplication().runWriteAction(() -> formatText(file, document, startOffset, endOffset, settings));
        } finally {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
            if (documentManager.isUncommited(document)) {
                documentManager.commitDocument(document);
            }
        }
    }

    /**
     * Formats the routes that could be found between the given indexes.
     *
     * @param file        the file that contains the text to format.
     * @param document    the document that contains the text to format.
     * @param startOffset the start offset of the text format
     * @param endOffset   the end offset of the text format
     * @param settings    the settings to apply when formatting the text.
     */
    private void formatText(PsiFile file, Document document, int startOffset, int endOffset,
                            CodeStyleSettings settings) {
        Module module = ModuleUtilCore.findModuleForPsiElement(file.getOriginalElement());
        List<PsiElement> endpointDeclarations = findEndpointDeclarations(module, e -> true)
            .stream()
            .filter(e -> file.equals(e.getContainingFile()))
            .sorted(Comparator.comparingInt(PsiElement::getTextOffset))
            .toList();
        Deque<Consumer<Document>> changes = new ArrayDeque<>();
        boolean useTabCharacter = settings.useTabCharacter(JavaFileType.INSTANCE);
        int indentSize = settings.getIndentSize(JavaFileType.INSTANCE);
        for (PsiElement endpoint : endpointDeclarations) {
            int textOffset = endpoint.getTextOffset();
            if (startOffset <= textOffset && textOffset < endOffset) {
                PsiExpressionStatement parent = PsiTreeUtil.getParentOfType(endpoint, PsiExpressionStatement.class);
                if (parent == null) {
                    LOG.debug("The parent PsiExpressionStatement of the element '%s' cannot be found".formatted(endpoint.getText()));
                    continue;
                }

                TextRange textRange = parent.getTextRange();
                CharSequence charsSequence = document.getCharsSequence();
                int textRangeStartOffset = textRange.getStartOffset();
                int textRangeEndOffset = textRange.getEndOffset();
                CharSequence contentToFormat = charsSequence.subSequence(textRangeStartOffset, textRangeEndOffset);
                CharSequence linePrefix = charsSequence.subSequence(document.getLineStartOffset(document.getLineNumber(textOffset)), textRangeStartOffset);
                CharSequence result = formatText(file, textRangeStartOffset, endOffset, contentToFormat, linePrefix, useTabCharacter, indentSize);
                // Ensure to apply the last changes first to avoid having to deal with offset change
                changes.addFirst(doc -> doc.replaceString(textRangeStartOffset, textRangeEndOffset, result));
            }
        }
        Consumer<Document> change = changes.poll();
        while (change != null) {
            change.accept(document);
            change = changes.poll();
        }
    }

    /**
     * Formats the given {@code CharSequence} corresponding to entire route written in Java DSL.
     *
     * @param file            the file in which the route has been defined.
     * @param startOffset     the start offset of the route definition.
     * @param endOffset       the limit of the offset beyond which the text can be formatted.
     * @param contentToFormat the content to format.
     * @param linePrefix      the first characters to add to a new line.
     * @param useTabCharacter indicates whether tab should be used to indent a line.
     * @param indentSize      the size of an ident in spaces.
     * @return the content of the route formatted according to the Java DSL.
     */
    private CharSequence formatText(PsiFile file, int startOffset, int endOffset, CharSequence contentToFormat,
                                    CharSequence linePrefix, boolean useTabCharacter, int indentSize) {
        StringBuilder result = new StringBuilder(contentToFormat.length());
        Matcher matcher = METHOD_CALL_PATTERN.matcher(contentToFormat);
        int indent = 0;
        int lastIndex = 0;
        Deque<String> stack = new ArrayDeque<>();
        String previousMethod = null;
        boolean lastProcessedMethodIsFormatted = false;
        while (matcher.find()) {
            String methodName = matcher.group(5);
            int startGroup = matcher.start(5);
            int startMatch = matcher.start();
            boolean hasDot = Objects.nonNull(matcher.group(3));
            int offset = startOffset + startGroup;
            if (endOffset <= offset) {
                // The limit is reached, no need to format beyond
                break;
            } else if (isMethodToFormat(file, offset)) {
                int currentIndent = indent;
                String lastMethodName = stack.peek();
                if (!hasDot && SUB_DSL_ROOTS.contains(methodName)) {
                    stack.push(methodName);
                    indent++;
                    if (Objects.nonNull(previousMethod) && !ADD_INDENT.contains(previousMethod) && !SUB_DSL_ROOTS.contains(previousMethod)
                        && !ADD_INDENT_OR_NEW_BLOCK.contains(previousMethod)) {
                        indent++;
                        currentIndent++;
                    }
                } else if (ADD_INDENT.contains(methodName) || ADD_INDENT_OR_NEW_BLOCK.contains(methodName) && !methodName.equals(lastMethodName)) {
                    stack.push(methodName);
                    indent++;
                } else if (REMOVE_INDENT.contains(methodName)) {
                    if (stack.size() > 1) {
                        stack.pop();
                        indent--;
                        currentIndent--;
                    } else {
                        LOG.debug("The method at index %d is invalid".formatted(offset));
                        break;
                    }
                } else if (NEW_BLOCK.contains(methodName) || ADD_INDENT_OR_NEW_BLOCK.contains(methodName) && methodName.equals(lastMethodName)) {
                    if (stack.size() > 1) {
                        currentIndent--;
                        stack.pop();
                        stack.push(methodName);
                    } else {
                        LOG.debug("The method at index %d is invalid".formatted(offset));
                        break;
                    }
                }
                result.append(contentToFormat.subSequence(lastIndex, startMatch));
                boolean hasDoubleParenthesis = Objects.nonNull(matcher.group(1));
                if (hasDoubleParenthesis) {
                    result.append("))");
                    if (hasDot && lastProcessedMethodIsFormatted) {
                        indent--;
                        currentIndent--;
                    }
                }
                boolean isShortPredicate = isShortPredicate(file, offset, result);
                if (currentIndent > 0 && !isShortPredicate) {
                    appendNewLine(result, linePrefix, useTabCharacter, indentSize, currentIndent);
                }
                if (hasDot) {
                    result.append('.');
                }
                result.append(methodName);
                result.append('(');
                lastIndex = matcher.end();
                previousMethod = methodName;
                lastProcessedMethodIsFormatted = !isShortPredicate && !isPredicateOfWhenClause(file, offset);
                continue;
            }
            lastProcessedMethodIsFormatted = false;
            result.append(contentToFormat.subSequence(lastIndex, matcher.end()));
            lastIndex = matcher.end();
        }
        result.append(contentToFormat.subSequence(lastIndex, contentToFormat.length()));
        return result;
    }

    /**
     * Appends a new line to the given {@code StringBuilder}
     *
     * @param builder         to builder to which the new line must be added.
     * @param linePrefix      the first characters to add to a new line.
     * @param useTabCharacter indicates whether tab should be used to indent a line.
     * @param indentSize      the size of an ident in spaces.
     * @param indent          the indentation to apply to the new line to append.
     */
    private static void appendNewLine(StringBuilder builder, CharSequence linePrefix, boolean useTabCharacter,
                                      int indentSize, int indent) {
        builder.append('\n');
        builder.append(linePrefix);
        for (int i = 0; i < indent; i++) {
            if (useTabCharacter) {
                builder.append('\t');
            } else {
                builder.append(" ".repeat(Math.max(0, indentSize)));
            }
        }
    }

    /**
     * Indicates whether the {@code PsiElement} located at the given {@code offset} is part of the methods to format.
     *
     * @param file   the file in which the method is located.
     * @param offset the offset of the method to check.
     * @return {@code true} if it is method to format, {@code false} otherwise.
     */
    private static boolean isMethodToFormat(PsiFile file, int offset) {
        return testMethod(file, offset, JavaCamelIdeaUtils::isMethodToFormat);
    }

    /**
     * Indicates whether the {@code PsiElement} located at the given {@code offset} is a method that matches with the
     * given predicate.
     *
     * @param file   the file in which the method is located.
     * @param offset the offset of the method to check.
     * @param predicate the predicate to evaluate
     * @return {@code true} if it is method matches with the predicate, {@code false} otherwise.
     */
    private static boolean testMethod(PsiFile file, int offset, Predicate<? super PsiMethod> predicate) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            LOG.debug("No element cannot be found at index %d".formatted(offset));
        } else if (element instanceof PsiIdentifier) {
            PsiCallExpression parent = PsiTreeUtil.getParentOfType(element, PsiCallExpression.class);
            if (parent == null) {
                LOG.debug("The parent PsiCallExpression of the element at index %d cannot be found".formatted(offset));
            } else if (parent instanceof PsiMethodCallExpression methodCallExpression) {
                JavaResolveResult[] results = methodCallExpression.getMethodExpression().multiResolve(false);
                if (results.length == 0) {
                    LOG.debug("The method corresponding to the element at index %d cannot be resolved".formatted(offset));
                } else if (Arrays.stream(results).map(JavaResolveResult::getElement)
                    .filter(PsiMethod.class::isInstance)
                    .map(PsiMethod.class::cast)
                    .anyMatch(predicate)) {
                    return true;
                } else {
                    LOG.trace("The method doesn't match with the predicate");
                }
            } else {
                LOG.debug("The parent PsiCallExpression of the element at index %d is not a method call".formatted(offset));
            }
        } else {
            LOG.trace("The element at index %d is not a PsiIdentifier".formatted(offset));
        }
        return false;
    }

    /**
     * Indicates whether the {@code PsiElement} located at the given {@code offset} is a predicate that can be inlined.
     *
     * @param file   the file in which the method is located.
     * @param offset the offset of the method to check.
     * @param currentContent the current formatted content
     * @return {@code true} if it is a predicate that can be inlined, {@code false} otherwise.
     */
    private static boolean isShortPredicate(PsiFile file, int offset, StringBuilder currentContent) {
        return checkPredicate(file, offset, methodName -> currentContent.lastIndexOf(methodName) > currentContent.lastIndexOf("\n"));
    }

    /**
     * Indicates whether the {@code PsiElement} located at the given {@code offset} is a predicate of a where clause.
     *
     * @param file   the file in which the method is located.
     * @param offset the offset of the method to check.
     * @return {@code true} if it is a predicate of a where clause, {@code false} otherwise.
     */
    private static boolean isPredicateOfWhenClause(PsiFile file, int offset) {
        return checkPredicate(file, offset, "when"::equals);
    }

    /**
     * Indicates whether the {@code PsiElement} located at the given {@code offset} is a Camel predicate that matches with the
     * given predicate.
     *
     * @param file   the file in which the method is located.
     * @param offset the offset of the method to check.
     * @param predicate the predicate to evaluate
     * @return {@code true} if it is method matches with the predicate, {@code false} otherwise.
     */
    private static boolean checkPredicate(PsiFile file, int offset, Predicate<String> predicate) {
        if (testMethod(file, offset, JavaCamelIdeaUtils::isPredicate)) {
            PsiExpressionList parent = PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiExpressionList.class);
            if (parent == null) {
                LOG.debug("The parent PsiExpressionList of the element at index %d cannot be found".formatted(offset));
            } else {
                PsiMethodCallExpression parentMethodCallExpression = PsiTreeUtil.getParentOfType(parent.getPrevSibling(), PsiMethodCallExpression.class);
                if (parentMethodCallExpression == null) {
                    LOG.debug("The parent method corresponding to the element at index %d cannot be resolved".formatted(offset));
                } else {
                    String methodName = parentMethodCallExpression.getMethodExpression().getReferenceName();
                    if (methodName == null) {
                        LOG.debug("The name of the parent method corresponding to the element at index %d cannot be found".formatted(offset));
                    } else {
                        return predicate.test(methodName);
                    }
                }
            }
        } else {
            LOG.trace("The element at index %d is not a Predicate".formatted(offset));
        }
        return false;
    }

    /**
     * Indicates whether the given class only contains methods to format.
     *
     * @param name the fully qualified name of the class to test
     * @return {@code true} if the class only contains method to format, {@code false} otherwise.
     */
    private static boolean isContainingClassOfMethodToFormat(String name) {
        // The class is part of the Java DSL and is not an expression
        return (name.startsWith("org.apache.camel.model.")
            || name.startsWith("org.apache.camel.builder.")
            || name.startsWith("org.apache.camel.support.builder."))
            && !"org.apache.camel.builder.ExpressionClause".equals(name);
    }

    /**
     * Indicates whether the given method with the given return type is a method to format.
     *
     * @param methodName the name of the method to check
     * @param returnType the type returned by the method
     * @return {@code true} if it is a method to format, {@code false} otherwise.
     */
    private static boolean isReturnTypeOfMethodToFormat(String methodName, String returnType) {
        if (returnType == null) {
            LOG.trace("The return type of the method cannot be found");
            // By default, it is accepted
            return true;
        }
        return !"org.apache.camel.builder.ValueBuilder".equals(returnType) || "expression".equals(methodName);
    }

    /**
     * Indicates whether the given method is a method to format.
     *
     * @param method the method to check
     * @return {@code true} if it is a method to format, {@code false} otherwise.
     */
    private static boolean isMethodToFormat(PsiMethod method) {
        String containingClass = getContainingClass(method);
        if (containingClass == null) {
            LOG.trace("The containing class of the method cannot be found");
            return false;
        }
        return isMethodToFormat(method.getName(), containingClass, getReturnType(method));
    }

    /**
     * Indicates whether the method with the given name, containing class and returned type is a method to format.
     *
     * @param name            the name of the method to check
     * @param containingClass the class containing the method
     * @param returnType      the type returned by the method
     * @return {@code true} if it is a method to format, {@code false} otherwise.
     */
    private static boolean isMethodToFormat(String name, String containingClass, String returnType) {
        return isContainingClassOfMethodToFormat(containingClass) && isReturnTypeOfMethodToFormat(name, returnType);
    }

    /**
     * Indicates whether the given method is a predicate
     *
     * @param method the method to check
     * @return {@code true} if the method is a predicate, {@code false} otherwise.
     */
    private static boolean isPredicate(PsiMethod method) {
        return "org.apache.camel.Predicate".equals(getReturnType(method));
    }

    /**
     * Gives the containing class of the given method.
     *
     * @param method the method for which the containing class is expected
     * @return the fully qualified name of the containing class if it could be found, {@code null} otherwise.
     */
    private static String getContainingClass(PsiMethod method) {
        PsiClass psiClass = method.getContainingClass();
        if (Objects.nonNull(psiClass)) {
            return psiClass.getQualifiedName();
        }
        LOG.trace("The containing class of the method cannot be found");
        return null;
    }

    /**
     * Gives the returned type of the given method
     *
     * @param method the method for which the returned type is expected
     * @return the fully qualified name of the returned type if it could be found, {@code null} otherwise.
     */
    private static String getReturnType(PsiMethod method) {
        PsiType psiType = method.getReturnType();
        if (psiType instanceof PsiClassReferenceType referenceType) {
            PsiClass returnType = referenceType.resolve();
            if (returnType == null) {
                LOG.trace("The return type of the method cannot be resolved");
            } else if (returnType instanceof PsiTypeParameter type) {
                PsiClass superClass = type.getSuperClass();
                if (superClass == null) {
                    LOG.trace("The super class of the type parameter of the method cannot be resolved");
                } else {
                    return superClass.getQualifiedName();
                }
            } else {
                return returnType.getQualifiedName();
            }
        } else {
            LOG.trace("The return type of the method is not a reference type");
        }
        return null;
    }

}
