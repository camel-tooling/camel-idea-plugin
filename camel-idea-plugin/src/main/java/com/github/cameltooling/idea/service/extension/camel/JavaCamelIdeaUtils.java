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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
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
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class JavaCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    private static final String JAVA_LANG_STRING = "java.lang.String";

    private static final List<String> JAVA_ROUTE_BUILDERS = Arrays.asList(
        new String[] {
            "RouteBuilder",
            "RoutesBuilder",
            "RouteConfigurationBuilder",
            "RouteConfigurationsBuilder",
            "AdviceWithRouteBuilder",
            "EndpointRouteBuilder",
            "EndpointRouteConfigurationBuilder",
            "SpringRouteBuilder"
        });

    @Override
    public boolean isCamelFile(PsiFile file) {
        if (file != null && file.getFileType().equals(JavaFileType.INSTANCE)) {
            PsiJavaFile javaFile = (PsiJavaFile) file;
            final PsiClass[] classes = javaFile.getClasses();
            for (PsiClass nextClass : classes) {
                PsiClassType[] extendsTypes = nextClass.getExtendsListTypes();
                for (PsiClassType nextType : extendsTypes) {
                    if (JAVA_ROUTE_BUILDERS.contains(nextType.getClassName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        return getIdeaUtils().isFromJavaMethodCall(element, true, ROUTE_START);
    }

    @Override
    public boolean isCamelRouteStartExpression(PsiElement element) {
        PsiElement routeStartParent = getIdeaUtils().findFirstParent(element, false,
                this::isCamelRouteStart, e -> e instanceof PsiFile);
        return routeStartParent != null;
    }

    @Override
    public boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart) {
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call == null) {
            return false;
        }
        if (!excludeRouteStart && getIdeaUtils().isFromJavaMethod(call, true, ROUTE_START)) {
            return true;
        }
        Collection<PsiMethodCallExpression> chainedCalls = PsiTreeUtil.findChildrenOfType(call, PsiMethodCallExpression.class);
        return chainedCalls.stream().anyMatch(c -> getIdeaUtils().isFromJavaMethod(c, true, ROUTE_START));
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
        if (getIdeaUtils().isFromJavaMethodCall(element, true, methods)) {
            return true;
        }
        return false;
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
            if (child instanceof PsiReferenceExpression) {
                // this code is needed as it may be used as a method call as a parameter and this requires
                // a bit of psi code to unwrap the right elements.
                PsiExpression exp = ((PsiReferenceExpression) child).getQualifierExpression();
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
                if (exp instanceof PsiMethodCallExpression) {
                    PsiMethod method = ((PsiMethodCallExpression) exp).resolveMethod();
                    if (method != null) {
                        String name = method.getName();
                        return Arrays.stream(PREDICATE_EIPS).anyMatch(name::equals);
                    }
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean isConsumerEndpoint(PsiElement element) {
        if (getIdeaUtils().isFromJavaMethodCall(element, true, CONSUMER_ENDPOINT)) {
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
        if (getIdeaUtils().isFromJavaMethodCall(element, true, PRODUCER_ENDPOINT)) {
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
        if (getIdeaUtils().isElementFromSetterProperty(element, "brokerURL")) {
            return true;
        }
        if (getIdeaUtils().isElementFromConstructor(element, "ActiveMQConnectionFactory")) {
            return true;
        }
        if (getIdeaUtils().isElementFromConstructor(element, "ActiveMQXAConnectionFactory")) {
            return true;
        }
        if (getIdeaUtils().isElementFromConstructor(element, "JmsConnectionFactory")) {
            return true;
        }
        if (getIdeaUtils().isElementFromAnnotation(element, "org.apache.camel.spi.UriEndpoint")) {
            return true;
        }
        if (getIdeaUtils().isFromJavaMethodCall(element, false, "activeMQComponent")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFromStringFormatEndpoint(PsiElement element) {
        if (getIdeaUtils().isFromJavaMethodCall(element, false, STRING_FORMAT_ENDPOINT)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean acceptForAnnotatorOrInspection(PsiElement element) {
        // skip XML limit on siblings
        boolean xml = getIdeaUtils().isFromFileType(element, "xml");
        if (!xml) {
            // for programming languages you can have complex structures with concat which we dont support yet
            int siblings = countSiblings(element);
            if (siblings > 1) {
                // we currently only support one liners, so check how many siblings the element has (it has 1 with ending parenthesis which is okay)
                return false;
            }
        }
        return true;
    }

    @Override
    public PsiClass getBeanClass(PsiElement element) {
        final PsiElement beanPsiElement = getPsiElementForCamelBeanMethod(element);
        if (beanPsiElement != null) {
            if (beanPsiElement instanceof PsiClass) {
                return (PsiClass) beanPsiElement;
            }

            PsiJavaCodeReferenceElement referenceElement = PsiTreeUtil.findChildOfType(beanPsiElement, PsiJavaCodeReferenceElement.class);
            final PsiClass psiClass = getJavaClassUtils().resolveClassReference(referenceElement);

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
        return findEndpoints(module, uriCondition, e -> isCamelRouteStart(e));
    }

    @Override
    public boolean isPlaceForEndpointUri(PsiElement location) {
        PsiLiteralExpression expression = PsiTreeUtil.getParentOfType(location, PsiLiteralExpression.class, false);
        return expression != null
                && isInsideCamelRoute(expression, false);
    }

    /**
     * @return the {@link PsiClass} for the matching bean name by looking for classes annotated with spring Component, Service or Repository
     */
    private Optional<PsiClass> searchForMatchingBeanClass(String beanName, Project project) {
        return getJavaClassUtils().findBeanClassByName(beanName, "org.springframework.stereotype.Component", project).map(Optional::of)
                .orElseGet(() -> getJavaClassUtils().findBeanClassByName(beanName, "org.springframework.stereotype.Service", project)).map(Optional::of)
                .orElseGet(() -> getJavaClassUtils().findBeanClassByName(beanName, "org.springframework.stereotype.Repository", project));
    }

    private List<PsiElement> findEndpoints(Module module, Predicate<String> uriCondition, Predicate<PsiLiteral> elementCondition) {
        PsiManager manager = PsiManager.getInstance(module.getProject());
        //TODO: use IdeaUtils.ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME somehow
        PsiClass routeBuilderClass = ClassUtil.findPsiClass(manager, "org.apache.camel.builder.RouteBuilder");

        List<PsiElement> results = new ArrayList<>();
        if (routeBuilderClass != null) {
            Collection<PsiClass> routeBuilders = ClassInheritorsSearch.search(routeBuilderClass, module.getModuleScope(), true)
                    .findAll();
            for (PsiClass routeBuilder : routeBuilders) {
                Collection<PsiLiteralExpression> literals = PsiTreeUtil.findChildrenOfType(routeBuilder, PsiLiteralExpression.class);
                for (PsiLiteralExpression literal : literals) {
                    Object val = literal.getValue();
                    if (val instanceof String) {
                        String endpointUri = (String) val;
                        if (uriCondition.test(endpointUri) && elementCondition.test(literal)) {
                            results.add(literal);
                        }
                    }
                }
            }
        }
        return results;
    }

    private String getStaticBeanName(PsiJavaCodeReferenceElement referenceElement, String beanName) {
        final PsiType type = ((PsiReferenceExpression) referenceElement).getType();
        if (type != null && JAVA_LANG_STRING.equals(type.getCanonicalText())) {
            beanName = StringUtils.stripDoubleQuotes(PsiTreeUtil.getChildOfAnyType(referenceElement.getReference().resolve(), PsiLiteralExpression.class).getText());
        }
        return beanName;
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

    private JavaClassUtils getJavaClassUtils() {
        return ServiceManager.getService(JavaClassUtils.class);
    }
}
