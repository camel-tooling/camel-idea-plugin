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
package org.apache.camel.idea.documentation;

import java.util.List;
import java.util.Map;

import com.intellij.ide.BrowserUtil;
import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.lang.documentation.ExternalDocumentationProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.apache.camel.idea.catalog.CamelCatalogService;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.ModelHelper;
import org.apache.camel.idea.util.CamelService;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.util.IdeaUtils.getInnerText;
import static org.apache.camel.idea.util.IdeaUtils.isStringLiteral;
import static org.apache.camel.idea.util.StringUtils.asComponentName;
import static org.apache.camel.idea.util.StringUtils.asLanguageName;
import static org.apache.camel.idea.util.StringUtils.wrapSeparator;

/**
 * Camel documentation provider to hook into IDEA to show Camel endpoint documentation in popups and various other places.
 */
public class CamelDocumentationProvider extends DocumentationProviderEx implements ExternalDocumentationProvider, ExternalDocumentationHandler {

    private static final String GITHUB_EXTERNAL_DOC_URL = "https://github.com/apache/camel/blob/master";

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return null;
    }

    @Nullable
    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        return null;
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            String val = fetchLiteralForCamelDocumentation(element);
            if (val == null) {
                return null;
            }

            Project project = element.getProject();
            CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
            String componentName = StringUtils.asComponentName(val);
            if (componentName != null) {
                return generateCamelComponentDocumentation(componentName, val, camelCatalog);
            } else {
                // its maybe a method call for a Camel language
                PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
                if (call != null) {
                    PsiMethod method = call.resolveMethod();
                    if (method != null) {
                        // try to see if we have a Camel language with the method name
                        String name = asLanguageName(method.getName());
                        if (camelCatalog.findLanguageNames().contains(name)) {
                            // okay its a potential Camel language so see if the psi method call is using
                            // camel-core types so we know for a fact its really a Camel language
                            if (isPsiMethodCamelLanguage(method)) {
                                String html = camelCatalog.languageHtmlDoc(name);
                                if (html != null) {
                                    return html;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement) {
        // documentation from properties file will cause IDEA to call this method where we can tell IDEA we can provide
        // documentation for the element if we can detect its a Camel component
        if (ServiceManager.getService(contextElement.getProject(), CamelService.class).isCamelPresent() && hasDocumentationForCamelComponent(contextElement)) {
            return contextElement;
        }
        return null;
    }

    @Nullable
    @Override
    public String fetchExternalDocumentation(Project project, PsiElement element, List<String> docUrls) {
        return null;
    }

    @Override
    public boolean hasDocumentationFor(PsiElement element, PsiElement originalElement) {
        return hasDocumentationForCamelComponent(element);
    }

    @Override
    public boolean canPromptToConfigureDocumentation(PsiElement element) {
        return false;
    }

    @Override
    public void promptToConfigureDocumentation(PsiElement element) {
        // noop
    }

    @Override
    public boolean handleExternal(PsiElement element, PsiElement originalElement) {
        String val = fetchLiteralForCamelDocumentation(element);
        if (val == null || ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            return false;
        }

        String name = StringUtils.asComponentName(val);
        Project project = element.getProject();
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
        if (name != null && camelCatalog.findComponentNames().contains(name)) {

            String json = camelCatalog.componentJSonSchema(name);
            ComponentModel component = ModelHelper.generateComponentModel(json, false);

            // to build external links which points to github
            String a = component.getArtifactId();

            String url;
            if ("camel-core".equals(a)) {
                url = GITHUB_EXTERNAL_DOC_URL + "/camel-core/src/main/docs/" + name + "-component.adoc";
            } else {
                url = GITHUB_EXTERNAL_DOC_URL + "/components/" + component.getArtifactId() + "/src/main/docs/" + name + "-component.adoc";
            }

            String hash = component.getTitle().toLowerCase().replace(' ', '-') + "-component";
            BrowserUtil.browse(url + "#" + hash);
            return true;
        }

        return false;
    }

    @Override
    public boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context) {
        return false;
    }

    @Override
    public boolean canFetchDocumentationLink(String link) {
        return false;
    }

    @NotNull
    @Override
    public String fetchExternalDocumentation(@NotNull String link, @Nullable PsiElement element) {
        return null;
    }

    private boolean hasDocumentationForCamelComponent(PsiElement element) {
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            String text = fetchLiteralForCamelDocumentation(element);
            if (text != null) {
                // check if its a known Camel component
                String name = asComponentName(text);
                Project project = element.getProject();
                return ServiceManager.getService(project, CamelCatalogService.class).get().findComponentNames().contains(name);
            }
        }
        return false;
    }

    private String fetchLiteralForCamelDocumentation(PsiElement element) {
        if (element == null) {
            return null;
        }

        if (isStringLiteral(element)) {
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            return (String) literal.getValue();
        }

        // is it from an xml attribute when using XML
        XmlAttributeValue xml = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
        if (xml != null) {
            return xml.getValue();
        }

        // its maybe a property from properties file
        String fqn = element.getClass().getName();
        if (fqn.startsWith("com.intellij.lang.properties.psi.impl.PropertyValue")) {
            // yes we can support this also
            return element.getText();
        }

        // maybe its yaml
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("yaml")) {
                return element.getText();
            }
        }

        // maybe its groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                String text = element.getText();
                // unwrap groovy gstring
                return getInnerText(text);
            }
        }

        // maybe its sccala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                String text = element.getText();
                // unwrap scala string
                return getInnerText(text);
            }
        }

        return null;
    }

    private String generateCamelComponentDocumentation(String componentName, String val, CamelCatalog camelCatalog) {
        // it is a known Camel component
        String json = camelCatalog.componentJSonSchema(componentName);
        ComponentModel component = ModelHelper.generateComponentModel(json, false);

        Map<String, String> existing = null;
        try {
            existing = camelCatalog.endpointProperties(val);
        } catch (Throwable e) {
            // ignore
        }

        StringBuilder options = new StringBuilder();
        if (existing != null && !existing.isEmpty()) {
            List<Map<String, String>> lines = JSonSchemaHelper.parseJsonSchema("properties", json, true);

            for (Map.Entry<String, String> entry : existing.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                Map<String, String> row = JSonSchemaHelper.getRow(lines, name);
                if (row != null) {
                    String kind = row.get("kind");

                    String line;
                    if ("path".equals(kind)) {
                        line = value + "<br/>";
                    } else {
                        line = name + "=" + value + "<br/>";
                    }
                    options.append("<br/>");
                    options.append("<b>").append(line).append("</b>");

                    String summary = row.get("description");
                    options.append(summary).append("<br/>");
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(component.getTitle()).append(" Component</b><br/>");
        sb.append(component.getDescription()).append("<br/><br/>");
        sb.append("Syntax: <tt>").append(component.getSyntax()).append("?options</tt><br/>");
        sb.append("Java class: <tt>").append(component.getJavaType()).append("</tt><br/>");

        String g = component.getGroupId();
        String a = component.getArtifactId();
        String v = component.getVersion();
        if (g != null && a != null && v != null) {
            sb.append("Maven: <tt>").append(g).append(":").append(a).append(":").append(v).append("</tt><br/>");
        }
        sb.append("<p/>");

        // indent the endpoint url with 5 spaces and wrap it by url separator
        String wrapped = wrapSeparator(val, "&", "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", 100);
        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>").append(wrapped).append("</b><br/>");

        if (options.length() > 0) {
            sb.append(options.toString());
        }
        return sb.toString();
    }

    private boolean isPsiMethodCamelLanguage(PsiMethod method) {
        PsiType type = method.getReturnType();
        if (type != null && type instanceof PsiClassReferenceType) {
            PsiClassReferenceType clazz = (PsiClassReferenceType) type;
            PsiClass resolved = clazz.resolve();
            if (resolved != null) {
                boolean language = isCamelExpressionOrLanguage(resolved);
                // try parent using some weird/nasty stub stuff which is how complex IDEA AST
                // is when its parsing the Camel route builder
                if (!language) {
                    PsiElement elem = resolved.getParent();
                    if (elem instanceof PsiTypeParameterList) {
                        elem = elem.getParent();
                    }
                    if (elem instanceof PsiClass) {
                        language = isCamelExpressionOrLanguage((PsiClass) elem);
                    }
                }
                return language;
            }
        }

        return false;
    }

    private boolean isCamelExpressionOrLanguage(PsiClass clazz) {
        if (clazz == null) {
            return false;
        }
        String fqn = clazz.getQualifiedName();
        if ("org.apache.camel.Expression".equals(fqn)
                || "org.apache.camel.Predicate".equals(fqn)
                || "org.apache.camel.model.language.ExpressionDefinition".equals(fqn)
                || "org.apache.camel.builder.ExpressionClause".equals(fqn)) {
            return true;
        }
        // try implements first
        for (PsiClassType ct : clazz.getImplementsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // then fallback as extends
        for (PsiClassType ct : clazz.getExtendsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // okay then go up and try super
        return isCamelExpressionOrLanguage(clazz.getSuperClass());
    }
}
