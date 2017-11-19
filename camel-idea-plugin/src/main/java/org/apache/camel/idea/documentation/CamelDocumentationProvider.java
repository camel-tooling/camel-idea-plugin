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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.lang.documentation.ExternalDocumentationProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.EndpointOptionModel;
import org.apache.camel.idea.model.ModelHelper;
import org.apache.camel.idea.service.CamelCatalogService;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.apache.camel.idea.util.IdeaUtils;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.util.StringUtils.asComponentName;
import static org.apache.camel.idea.util.StringUtils.asLanguageName;
import static org.apache.camel.idea.util.StringUtils.wrapSeparator;
import static org.apache.camel.idea.util.StringUtils.wrapWords;

/**
 * Camel documentation provider to hook into IDEA to show Camel endpoint documentation in popups and various other places.
 */
public class CamelDocumentationProvider extends DocumentationProviderEx implements ExternalDocumentationProvider, ExternalDocumentationHandler {

    private static final Logger LOG = Logger.getInstance(CamelDocumentationProvider.class);

    private static final String GITHUB_EXTERNAL_DOC_URL = "https://github.com/apache/camel/blob/master";

    public IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

    public CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            PsiExpressionList exps = PsiTreeUtil.getNextSiblingOfType(originalElement, PsiExpressionList.class);
            if (exps != null) {
                if (exps.getExpressions().length >= 1) {
                    // grab first string parameter (as the string would contain the camel endpoint uri
                    final PsiClassType stringType = PsiType.getJavaLangString(element.getManager(), element.getResolveScope());
                    PsiExpression exp = Arrays.stream(exps.getExpressions()).filter(
                        e -> e.getType() != null && stringType.isAssignableFrom(e.getType()))
                        .findFirst().orElse(null);
                    if (exp instanceof PsiLiteralExpression) {
                        Object o = ((PsiLiteralExpression) exp).getValue();
                        String val = o != null ? o.toString() : null;
                        // okay only allow this popup to work when its from a RouteBuilder class
                        PsiClass clazz = PsiTreeUtil.getParentOfType(originalElement, PsiClass.class);
                        if (clazz != null) {
                            PsiClassType[] types = clazz.getExtendsListTypes();
                            boolean found = Arrays.stream(types).anyMatch(p -> p.getClassName().equals("RouteBuilder"));
                            if (found) {
                                String componentName = StringUtils.asComponentName(val);
                                if (componentName != null) {
                                    // the quick info cannot be so wide so wrap at 120 chars
                                    return generateCamelComponentDocumentation(componentName, val, 120, element.getProject());
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
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        return null;
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof DocumentationElement) {
            DocumentationElement documentationElement = (DocumentationElement) element;
            return generateCamelEndpointOptionDocumentation(documentationElement.getComponentName(), documentationElement.getEndpointOption(), element.getProject());
        }

        String val = null;
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            val = fetchLiteralForCamelDocumentation(element);
            if (val == null) {
                return null;
            }
        }

        String componentName = StringUtils.asComponentName(val);
        if (componentName != null) {
            return generateCamelComponentDocumentation(componentName, val, -1, element.getProject());
        } else {
            // its maybe a method call for a Camel language
            PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
            if (call != null) {
                PsiMethod method = call.resolveMethod();
                if (method != null) {
                    // try to see if we have a Camel language with the method name
                    String name = asLanguageName(method.getName());
                    if (ServiceManager.getService(element.getProject(), CamelCatalogService.class).get().findLanguageNames().contains(name)) {
                        // okay its a potential Camel language so see if the psi method call is using
                        // camel-core types so we know for a fact its really a Camel language
                        if (isPsiMethodCamelLanguage(method)) {
                            String html = ServiceManager.getService(element.getProject(), CamelCatalogService.class).get().languageHtmlDoc(name);
                            if (html != null) {
                                return html;
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
        // we only support literal - string types where Camel endpoints can be specified
        if (object == null || !(object instanceof String)) {
            return null;
        }

        String lookup = object.toString();

        // must be a Camel component
        String componentName = StringUtils.asComponentName(lookup);
        if (componentName == null) {
            return null;
        }

        // unescape xml &
        lookup = lookup.replaceAll("&amp;", "&");

        // get last option from lookup line
        int pos = Math.max(lookup.lastIndexOf("&"), lookup.lastIndexOf("?"));
        if (pos > 0) {
            String option = lookup.substring(pos + 1);
            // if the option has a value then drop that
            pos = option.indexOf("=");
            if (pos != -1) {
                option = option.substring(0, pos);
            }
            LOG.debug("getDocumentationElementForLookupItem: " + option);

            // if the option ends with a dot then its a prefixed/multi value option which we need special logic
            // find its real option name and documentation which we want to show in the quick doc window
            if (option.endsWith(".")) {
                CamelCatalog camelCatalog = ServiceManager.getService(psiManager.getProject(), CamelCatalogService.class).get();
                String json = camelCatalog.componentJSonSchema(componentName);
                if (json == null) {
                    return null;
                }
                ComponentModel component = ModelHelper.generateComponentModel(json, true);

                final String prefixOption = option;

                // find the line with this prefix as prefix and multivalue
                EndpointOptionModel endpointOption = component.getEndpointOptions().stream().filter(
                    o -> "true".equals(o.getMultiValue()) && prefixOption.equals(o.getPrefix()))
                    .findFirst().orElse(null);

                // use the real option name instead of the prefix
                if (endpointOption != null) {
                    option = endpointOption.getName();
                }
            }

            return new DocumentationElement(psiManager, element.getLanguage(), element, option, componentName);
        }

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

        if (contextElement != null) {
            ASTNode node = contextElement.getNode();
            if (node != null && node instanceof XmlToken) {
                //there is an &amp; in the route that splits the route in separated PsiElements
                if (node.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                    //the caret is at the end of the route next to the " character
                    || node.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER
                    //the caret is placed on an &amp; element
                    || contextElement.getText().equals("&amp;")) {
                    if (hasDocumentationForCamelComponent(contextElement.getParent())) {
                        return contextElement.getParent();
                    }
                }
            }
            if (hasDocumentationForCamelComponent(contextElement)) {
                return contextElement;
            }
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
        if (val == null || !ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            return false;
        }

        String name = StringUtils.asComponentName(val);
        Project project = element.getProject();
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
        if (name != null && camelCatalog.findComponentNames().contains(name)) {

            String json = camelCatalog.componentJSonSchema(name);
            ComponentModel component = ModelHelper.generateComponentModel(json, false);

            // to build external links which points to github
            String artifactId = component.getArtifactId();

            String url;
            if ("camel-core".equals(artifactId)) {
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
        return getIdeaUtils().extractTextFromElement(element);
    }

    /**
     * Generates documentation for the endpoint option.
     * @param componentName the name of the Camel component
     * @param option the name of the Camel component option to generate documentation for
     * @param project the current project
     * @return a String representing the HTML documentation
     */
    private String generateCamelEndpointOptionDocumentation(String componentName, String option, Project project) {
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
        String json = camelCatalog.componentJSonSchema(componentName);
        if (json == null) {
            return null;
        }
        ComponentModel component = ModelHelper.generateComponentModel(json, true);

        EndpointOptionModel endpointOption;
        if (option.endsWith(".")) {
            // find the line with this prefix as prefix and multivalue
            endpointOption = component.getEndpointOptions().stream().filter(
                o -> "true".equals(o.getMultiValue()) && option.equals(o.getPrefix()))
                .findFirst().orElse(null);
        } else {
            endpointOption = component.getEndpointOption(option);
        }
        if (endpointOption == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<strong>").append(endpointOption.getName()).append("</strong><br/><br/>");
        builder.append("<strong>Group: </strong>").append(endpointOption.getGroup()).append("<br/>");
        builder.append("<strong>Type: </strong>").append("<tt>").append(endpointOption.getJavaType()).append("</tt>").append("<br/>");
        boolean required = false;
        if (!endpointOption.getRequired().equals("")) {
            required = true;
        }
        builder.append("<strong>Required: </strong>").append(required).append("<br/>");
        if (!endpointOption.getEnums().equals("")) {
            builder.append("<strong>Possible values: </strong>").append(endpointOption.getEnums().replace(",", ", ")).append("<br/>");
        }
        if (!endpointOption.getDefaultValue().equals("")) {
            builder.append("<strong>Default value: </strong>").append(endpointOption.getDefaultValue()).append("<br/>");
        }
        builder.append("<br/><div>").append(endpointOption.getDescription()).append("</div>");
        return builder.toString();
    }

    private String generateCamelComponentDocumentation(String componentName, String val, int wrapLength, Project project) {
        // it is a known Camel component
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
        String json = camelCatalog.componentJSonSchema(componentName);
        if (json == null) {
            return null;
        }

        ComponentModel component = ModelHelper.generateComponentModel(json, false);

        // camel catalog expects &amp; as & when it parses so replace all &amp; as &
        String camelQuery = val;
        camelQuery = camelQuery.replaceAll("&amp;", "&");

        // strip up ending incomplete parameter
        if (camelQuery.endsWith("&") || camelQuery.endsWith("?")) {
            camelQuery = camelQuery.substring(0, camelQuery.length() - 1);
        }

        Map<String, String> existing = null;
        try {
            existing = camelCatalog.endpointProperties(camelQuery);
        } catch (Throwable e) {
            LOG.warn("Error parsing Camel endpoint properties with url: " + camelQuery, e);
        }

        StringBuilder options = new StringBuilder();
        if (existing != null && !existing.isEmpty()) {
            List<Map<String, String>> lines = JSonSchemaHelper.parseJsonSchema("properties", json, true);

            for (Map.Entry<String, String> entry : existing.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                Map<String, String> row;

                // is it a multi valued option then we need to find the option name to use for lookup
                String option = JSonSchemaHelper.getPropertyNameFromNameWithPrefix(lines, name);
                if (option != null && JSonSchemaHelper.isPropertyMultiValue(lines, option)) {
                    row = JSonSchemaHelper.getRow(lines, option);
                } else {
                    row = JSonSchemaHelper.getRow(lines, name);
                }

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
                    // the text looks a bit weird when using single /
                    summary = summary.replace('/', ' ');
                    options.append(wrapText(summary, wrapLength)).append("<br/>");
                }
            }
        }

        // append any lenient options as well
        Map<String, String> extra = null;
        try {
            extra = camelCatalog.endpointLenientProperties(camelQuery);
        } catch (Throwable e) {
            LOG.warn("Error parsing Camel endpoint properties with url: " + camelQuery, e);
        }
        if (extra != null && !extra.isEmpty()) {
            for (Map.Entry<String, String> entry : extra.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                String line = name + "=" + value + "<br/>";
                options.append("<br/>");
                options.append("<b>").append(line).append("</b>");

                String summary = "This option is a custom option that is not part of the Camel component";
                options.append(wrapText(summary, wrapLength)).append("<br/>");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(component.getTitle()).append(" Component</b><br/>");
        sb.append(wrapText(component.getDescription(), wrapLength)).append("<br/><br/>");
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
                boolean language = getCamelIdeaUtils().isCamelExpressionOrLanguage(resolved);
                // try parent using some weird/nasty stub stuff which is how complex IDEA AST
                // is when its parsing the Camel route builder
                if (!language) {
                    PsiElement elem = resolved.getParent();
                    if (elem instanceof PsiTypeParameterList) {
                        elem = elem.getParent();
                    }
                    if (elem instanceof PsiClass) {
                        language = getCamelIdeaUtils().isCamelExpressionOrLanguage((PsiClass) elem);
                    }
                }
                return language;
            }
        }

        return false;
    }

    private static String wrapText(String text, int wrapLength) {
        if (wrapLength > 0) {
            text = wrapWords(text, "<br/>", wrapLength, true);
        }
        return text;
    }

    /**
     * {@link PsiElement} used only to transfer documentation data.
     */
    static class DocumentationElement extends LightElement {
        private PsiElement element;
        private String endpointOption;
        private String componentName;

        DocumentationElement(@NotNull PsiManager psiManager, @NotNull Language language, PsiElement element, String endpointOption, String componentName) {
            super(psiManager, language);
            this.element = element;
            this.endpointOption = endpointOption;
            this.componentName = componentName;
        }

        @Override
        public String toString() {
            return element.getText();
        }

        @Override
        public String getText() {
            return endpointOption;
        }

        public PsiElement getElement() {
            return element;
        }

        String getEndpointOption() {
            return endpointOption;
        }

        String getComponentName() {
            return componentName;
        }
    }
}
