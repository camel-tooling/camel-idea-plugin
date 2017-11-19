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
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.apache.camel.idea.extension.CamelIdeaUtilsExtension;
import org.apache.camel.idea.util.IdeaUtils;
import org.apache.camel.idea.util.StringUtils;


public class XmlCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        if (element.getText().equals("from")) {
            XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
            if (xml != null) {
                String name = xml.getLocalName();
                XmlTag parentTag = xml.getParentTag();
                if (parentTag != null) {
                    return "from".equals(name) && "route".equals(parentTag.getLocalName());
                }
            }
        }
        return false;
    }

    @Override
    public boolean isCamelSimpleExpression(PsiElement element) {
        // xml
        XmlTag xml;
        if (element instanceof XmlTag) {
            xml = (XmlTag) element;
        } else {
            xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        }
        if (xml != null) {
            String name = xml.getLocalName();
            return "simple".equals(name) || "log".equals(name);
        }
        return false;
    }

    @Override
    public boolean isCameSimpleExpressionUsedAsPredicate(PsiElement element) {
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            // if its coming from the log EIP then its not a predicate
            if (xml.getLocalName().equals("log")) {
                return false;
            }

            // special for loop which can be both expression or predicate
            if (getIdeaUtils().hasParentXmlTag(xml, "loop")) {
                XmlTag parent = PsiTreeUtil.getParentOfType(xml, XmlTag.class);
                if (parent != null) {
                    String doWhile = parent.getAttributeValue("doWhile");
                    return "true".equalsIgnoreCase(doWhile);
                }
            }
            return Arrays.stream(SIMPLE_PREDICATE).anyMatch(n -> getIdeaUtils().hasParentXmlTag(xml, n));
        }
        return false;
    }

    @Override
    public boolean isConsumerEndpoint(PsiElement element) {
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            return getIdeaUtils().hasParentXmlTag(xml, "pollEnrich")
                || getIdeaUtils().isFromXmlTag(xml, "from", "interceptFrom");
        }

        return false;
    }

    @Override
    public boolean isProducerEndpoint(PsiElement element) {
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            return getIdeaUtils().hasParentXmlTag(xml, "enrich")
                || getIdeaUtils().isFromXmlTag(xml, "to", "interceptSendToEndpoint", "wireTap", "deadLetterChannel");
        }

        return false;
    }

    @Override
    public boolean skipEndpointValidation(PsiElement element) {
        // only accept xml tags from namespaces we support
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            String ns = xml.getNamespace();
            // accept empty namespace which can be from testing
            boolean accepted = StringUtils.isEmpty(ns) || Arrays.stream(ACCEPTED_NAMESPACES).anyMatch(ns::contains);
            LOG.trace("XmlTag " + xml.getName() + " with namespace: " + ns + " is accepted namespace: " + accepted);
            return !accepted; // skip is the opposite
        }

        return false;
    }

    @Override
    public boolean isFromStringFormatEndpoint(PsiElement element) {
        return false;
    }

    @Override
    public boolean acceptForAnnotatorOrInspection(PsiElement element) {
        return false;
    }

    @Override
    public boolean isExtensionEnabled() {
        return true;
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }
}
