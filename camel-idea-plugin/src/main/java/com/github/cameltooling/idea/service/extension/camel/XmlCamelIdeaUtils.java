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
package com.github.cameltooling.idea.service.extension.camel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.xml.util.XmlUtil;

public class XmlCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        if (element instanceof XmlTag) {
            return isCamelRouteStartTag((XmlTag) element);
        } else if (element.getText().equals("from") || element.getText().equals("rest")) {
            XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
            boolean xmlEndTag = element.getPrevSibling().getText().equals("</");
            if (xml != null && !xmlEndTag) {
                return isCamelRouteStartTag(xml);
            }
        }
        return false;
    }

    @Override
    public boolean isCamelRouteStartExpression(PsiElement element) {
        boolean textualXmlToken = element instanceof XmlToken
            && !element.getText().equals("<")
            && !element.getText().equals("</")
            && !element.getText().equals(">")
            && !element.getText().equals("/>");
        return textualXmlToken && isCamelRouteStart(element);
    }

    private boolean isCamelRouteStartTag(XmlTag tag) {
        String name = tag.getLocalName();
        XmlTag parentTag = tag.getParentTag();
        if (parentTag != null) {
            //TODO: unsure about this, <rest> cannot be a child of <routes> according to blueprint xsd, see issue #475
            return "routes".equals(parentTag.getLocalName()) && "rest".equals(name)
                || "route".equals(parentTag.getLocalName()) && "from".equals(name);
        }
        return false;
    }

    @Override
    public boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart) {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (tag == null || (excludeRouteStart && isCamelRouteStartTag(tag))) {
            return false;
        }
        PsiElement routeTag = getIdeaUtils().findFirstParent(tag, false, this::isCamelRouteTag, e -> e instanceof PsiFile);
        return routeTag != null;
    }

    @Override
    public List<PsiElement> findEndpointUsages(Module module, Predicate<String> uriCondition) {
        return findEndpoints(module, uriCondition, e -> !isCamelRouteStart(e));
    }

    @Override
    public List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition) {
        return findEndpoints(module, uriCondition, e -> isCamelRouteStart(e));
    }

    private List<PsiElement> findEndpoints(Module module, Predicate<String> uriCondition, Predicate<XmlTag> tagCondition) {
        Predicate<XmlAttributeValue> endpointMatcher =
            ((Predicate<XmlAttributeValue>)this::isEndpointUriValue)
            .and(e -> parentTagMatches(e, tagCondition))
            .and(e -> uriCondition.test(e.getValue()));

        List<PsiElement> endpointDeclarations = new ArrayList<>();
        iterateXmlDocuments(module, document -> {
            XmlTag root = document.getRootTag();
            if (root == null) {
                return;
            }
            iterateXmlNodes(root, XmlAttributeValue.class, e -> {
                if (endpointMatcher.test(e)) {
                    endpointDeclarations.add(e);
                }
            });
        });
        return endpointDeclarations;
    }

    private boolean parentTagMatches(PsiElement element, Predicate<XmlTag> parentTagCondition) {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        return tag != null && parentTagCondition.test(tag);
    }

    private boolean isEndpointUriValue(XmlAttributeValue endpointUriValue) {
        XmlAttribute attribute = PsiTreeUtil.getParentOfType(endpointUriValue, XmlAttribute.class);
        return attribute != null && attribute.getLocalName().equals("uri");
    }

    private boolean isCamelRouteTag(PsiElement element) {
        if (element instanceof XmlTag) {
            XmlTag routeTag = (XmlTag) element;
            return routeTag.getLocalName().equals("route");
        } else {
            return false;
        }
    }

    private void iterateXmlDocuments(Module module, Consumer<XmlDocument> xmlFileConsumer) {
        final GlobalSearchScope moduleScope = module.getModuleScope(true);
        final GlobalSearchScope xmlFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(moduleScope, XmlFileType.INSTANCE);

        ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        fileIndex.iterateContent(f -> {
            if (xmlFiles.contains(f)) {
                PsiFile file = PsiManager.getInstance(module.getProject()).findFile(f);
                if (file instanceof XmlFile) {
                    XmlFile xmlFile = (XmlFile) file;
                    XmlTag root = xmlFile.getRootTag();
                    if (root != null) {
                        if (isAcceptedNamespace(root.getNamespace())) {
                            xmlFileConsumer.accept(xmlFile.getDocument());
                        }
                    }
                }
            }
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void iterateXmlNodes(XmlTag root, Class<T> nodeClass, Consumer<T> nodeConsumer) {
        XmlUtil.processXmlElementChildren(root, element -> {
            if (nodeClass.isAssignableFrom(element.getClass())) {
                nodeConsumer.accept((T) element);
            }
            return true;
        }, true);
    }

    @Override
    public boolean isCamelExpression(PsiElement element, String language) {
        // xml
        XmlTag xml;
        if (element instanceof XmlTag) {
            xml = (XmlTag) element;
        } else {
            xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        }
        if (xml != null) {
            String name = xml.getLocalName();
            // extra check for simple language
            if ("simple".equals(language) && "log".equals(name)) {
                return true;
            }
            return language.equals(name);
        }
        return false;
    }

    @Override
    public boolean isCamelExpressionUsedAsPredicate(PsiElement element, String language) {
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            // if its coming from the log EIP then its not a predicate
            if ("simple".equals(language) && xml.getLocalName().equals("log")) {
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
            return Arrays.stream(PREDICATE_EIPS).anyMatch(n -> getIdeaUtils().hasParentXmlTag(xml, n));
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
            boolean accepted = StringUtils.isEmpty(ns) || isAcceptedNamespace(ns);
            LOG.trace("XmlTag " + xml.getName() + " with namespace: " + ns + " is accepted namespace: " + accepted);
            return !accepted; // skip is the opposite
        }

        return false;
    }

    private boolean isAcceptedNamespace(String ns) {
        return Arrays.stream(ACCEPTED_NAMESPACES).anyMatch(ns::contains);
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
    public PsiClass getBeanClass(PsiElement element) {
        if (element instanceof XmlToken) {

        }
        return null;
    }

    @Override
    public PsiElement getPsiElementForCamelBeanMethod(PsiElement element) {
        return null;
    }

    @Override
    public boolean isPlaceForEndpointUri(PsiElement location) {
        XmlAttributeValue value = PsiTreeUtil.getParentOfType(location, XmlAttributeValue.class, false);
        if (value == null) {
            return false;
        }
        XmlAttribute attr = PsiTreeUtil.getParentOfType(location, XmlAttribute.class);
        if (attr == null) {
            return false;
        }
        return attr.getLocalName().equals("uri") && isInsideCamelRoute(location, false);
    }

    @Override
    public boolean isExtensionEnabled() {
        return true;
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }
}
