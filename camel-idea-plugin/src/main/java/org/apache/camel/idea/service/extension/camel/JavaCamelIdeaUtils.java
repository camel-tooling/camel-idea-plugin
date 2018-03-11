/**
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
package org.apache.camel.idea.service.extension.camel;

import java.util.Arrays;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.camel.idea.extension.CamelIdeaUtilsExtension;
import org.apache.camel.idea.util.IdeaUtils;


public class JavaCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        if (getIdeaUtils().isFromJavaMethodCall(element, true, ROUTE_START)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isCamelSimpleExpression(PsiElement element) {
        // java method call
        if (getIdeaUtils().isFromJavaMethodCall(element, true, "simple", "log")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isCamelJSonPathExpression(PsiElement element) {
        // java method call
        if (getIdeaUtils().isFromJavaMethodCall(element, true, "jsonpath")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isCameSimpleExpressionUsedAsPredicate(PsiElement element) {
        // java
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {

            PsiMethod method = call.resolveMethod();
            if (method != null) {
                // if its coming from the log EIP then its not a predicate
                String name = method.getName();
                if ("log".equals(name)) {
                    return false;
                }
            }

            // okay dive into the psi and find out which EIP are using the simple
            PsiElement child = call.getFirstChild();
            if (child instanceof PsiReferenceExpression) {
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
                    method = ((PsiMethodCallExpression) exp).resolveMethod();
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
    public boolean isCameJSonPathExpressionUsedAsPredicate(PsiElement element) {
        // java
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            // okay dive into the psi and find out which EIP are using the simple
            PsiElement child = call.getFirstChild();
            if (child instanceof PsiReferenceExpression) {
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
        final PsiElement beanPsiElement = getBeanPsiElement(element);
        if (beanPsiElement != null) {
            PsiElement resolve = beanPsiElement.getReference().resolve();
            if (resolve instanceof PsiClass) {
                return (PsiClass) resolve;
            }
        }
        return null;
    }

    @Override
    public PsiElement getBeanPsiElement(PsiElement element) {
        if (element instanceof PsiLiteral || element.getParent() instanceof PsiLiteralExpression) {
            final PsiExpressionList expressionList = PsiTreeUtil.getParentOfType(element, PsiExpressionList.class);
            if (expressionList != null) {
                final PsiIdentifier identifier = PsiTreeUtil.getChildOfType(expressionList.getPrevSibling(), PsiIdentifier.class);
                if (identifier != null && identifier.getNextSibling() == null && "bean".equals(identifier.getText())) {
                    return PsiTreeUtil.findChildOfType(expressionList, PsiJavaCodeReferenceElement.class);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isExtensionEnabled() {
        return true;
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }
}
