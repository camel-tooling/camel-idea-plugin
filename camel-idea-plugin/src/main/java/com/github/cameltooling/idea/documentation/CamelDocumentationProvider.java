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
package com.github.cameltooling.idea.documentation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.cameltooling.idea.completion.OptionSuggestion;
import com.github.cameltooling.idea.completion.SimpleSuggestion;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.JavaClassUtils;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.lang.documentation.ExternalDocumentationProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static com.github.cameltooling.idea.util.StringUtils.asComponentName;
import static com.github.cameltooling.idea.util.StringUtils.wrapSeparator;
import static com.github.cameltooling.idea.util.StringUtils.wrapWords;

/**
 * Camel documentation provider to hook into IDEA to show Camel endpoint documentation in popups and various other places.
 */
public class CamelDocumentationProvider extends DocumentationProviderEx implements ExternalDocumentationProvider, ExternalDocumentationHandler {

    private static final Logger LOG = Logger.getInstance(CamelDocumentationProvider.class);

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
                                String componentName = asComponentName(val);
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
        if (hasDocumentationForCamelComponent(element)) {
            String val = fetchLiteralForCamelDocumentation(element);
            String url = externalUrl(element.getProject(), val);
            if (url != null) {
                return List.of(url);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof DocumentationElement) {
            DocumentationElement documentationElement = (DocumentationElement) element;
            return generateCamelEndpointOptionDocumentation(documentationElement.getComponentName(), documentationElement.getEndpointOption(), element.getProject());
        } else if (element instanceof DocumentationAsString) {
            return element.toString();
        }

        String val = null;
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            val = fetchLiteralForCamelDocumentation(element);
            if (val == null) {
                return null;
            }
        }

        String componentName = asComponentName(val);
        if (componentName != null) {
            return generateCamelComponentDocumentation(componentName, val, -1, element.getProject());
        }
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        if (object instanceof OptionSuggestion) {
            return new OptionDocumentationElement(psiManager, element.getLanguage(), (OptionSuggestion) object);
        } else if (object instanceof SimpleSuggestion) {
            return new SimpleSuggestionDocumentationElement(psiManager, element.getLanguage(), (SimpleSuggestion) object);
        }
        // we only support literal - string types where Camel endpoints can be specified
        if (!(object instanceof String)) {
            return null;
        }

        String lookup = object.toString();

        // must be a Camel component
        String componentName = asComponentName(lookup);
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
                ComponentModel component = JsonMapper.generateComponentModel(json);

                final String prefixOption = option;

                // find the line with this prefix as prefix and multivalue
                ComponentModel.EndpointOptionModel endpointOption = component.getEndpointOptions().stream().filter(
                    o -> o.isMultiValue() && prefixOption.equals(o.getPrefix()))
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

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        // documentation from properties file will cause IDEA to call this method where we can tell IDEA we can provide
        // documentation for the element if we can detect its a Camel component

        if (contextElement != null) {
            ASTNode node = contextElement.getNode();
            if (node instanceof XmlToken) {
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

    @Override
    public @Nullable String fetchExternalDocumentation(Project project, PsiElement element, List<String> docUrls, boolean onHover) {
        // F1 documentation which is external but shown inside IDEA

        // need to be run as read-action to avoid IDEA reporting an error
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> generateDoc(element, element));
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
        String url = externalUrl(element.getProject(), val);
        if (url != null) {
            BrowserUtil.browse(url);
            return true;
        }
        return false;
    }

    private static String externalUrl(Project project, String val) {
        String url = null;
        String name = asComponentName(val);
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
        if (name != null && camelCatalog.findComponentNames().contains(name)) {
            String json = camelCatalog.componentJSonSchema(name);
            ComponentModel component = JsonMapper.generateComponentModel(json);
            String version = component.getVersion();
            if (version.startsWith("2")) {
                version = "2.x";
            } else if (version.startsWith("3.4")) {
                version = "3.4.x"; // LTS
            } else if (version.startsWith("3.7")) {
                version = "3.7.x"; // LTS
            } else {
                version = "latest";
            }
            if ("other".equals(component.getKind())) {
                url = String.format("https://camel.apache.org/components/%s/others/%s.html", version, component.getName());
            } else if ("component".equals(component.getKind())) {
                url = String.format("https://camel.apache.org/components/%s/%s-component.html", version, component.getScheme());
            } else {
                url = String.format("https://camel.apache.org/components/%s/%ss/%s-%s.html", version, component.getKind(), component.getName(), component.getKind());
            }
        }
        return url;
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
    @Nullable
    private String generateCamelEndpointOptionDocumentation(String componentName, String option, Project project) {
        CamelCatalog camelCatalog = project.getService(CamelCatalogService.class).get();
        ComponentModel component = camelCatalog.componentModel(componentName);
        if (component == null) {
            return null;
        }
        ComponentModel.EndpointOptionModel endpointOption;
        if (option.endsWith(".")) {
            // find the line with this prefix as prefix and multivalue
            endpointOption = component.getEndpointOptions().stream().filter(
                o -> o.isMultiValue() && option.equals(o.getPrefix()))
                .findFirst().orElse(null);
        } else {
            endpointOption = component.getEndpointOptions().stream().filter(
                o -> option.equals(o.getName()))
                .findFirst().orElse(null);
        }
        return generateCamelOptionDocumentation(endpointOption);
    }

    /**
     * Generate the documentation of a given option.
     * @param option the option for which we expect the documentation.
     * @return the documentation corresponding to the given option. {@code null} if the given option is also
     * {@code null}.
     */
    @Nullable
    private static String generateCamelOptionDocumentation(@Nullable BaseOptionModel option) {
        if (option == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        if (option.isDeprecated()) {
            builder.append("<strong><s>").append(option.getName()).append("</s></strong><br/><br/>");
        } else {
            builder.append("<strong>").append(option.getName()).append("</strong><br/><br/>");
        }
        builder.append("<strong>Group: </strong>").append(Optional.ofNullable(option.getGroup()).orElse("NA")).append("<br/>");
        builder.append("<strong>Type: </strong>").append("<tt>").append(JavaClassUtils.getService().toSimpleType(option.getJavaType())).append("</tt>").append("<br/>");
        boolean required = option.isRequired();
        builder.append("<strong>Required: </strong>").append(required).append("<br/>");
        if (option.getEnums() != null) {
            String values = String.join(", ", option.getEnums());
            builder.append("<strong>Possible values: </strong>").append(values).append("<br/>");
        }
        if (option.getDefaultValue() != null) {
            builder.append("<strong>Default value: </strong>").append(option.getDefaultValue()).append("<br/>");
        }
        builder.append("<br/><div>").append(option.getDescription()).append("</div>");
        return builder.toString();
    }

    /**
     * Generate the documentation of a given suggestion.
     * @param suggestion the suggestion for which we expect the documentation.
     */
    @Nullable
    private static String generateCamelSimpleDocumentation(@NotNull SimpleSuggestion suggestion) {
        final String description = suggestion.getDescription();
        if (description == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("<strong>").append(suggestion.getName()).append("</strong>");
        builder.append("<br/><br/><div>").append(description).append("</div>");
        return builder.toString();
    }

    private String generateCamelComponentDocumentation(String componentName, String val, int wrapLength, Project project) {
        // it is a known Camel component
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();
        String json = camelCatalog.componentJSonSchema(componentName);
        if (json == null) {
            return null;
        }

        ComponentModel component = JsonMapper.generateComponentModel(json);

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
            JsonObject jsonObject;
            try {
                jsonObject = (JsonObject) Jsoner.deserialize(json);
            } catch (DeserializationException e) {
                throw new RuntimeException(e);
            }
            Map<String, JsonObject> properties = jsonObject.getMap("properties");

            for (Map.Entry<String, String> entry : existing.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                JsonObject row = properties.get(name);

                if (row != null) {
                    String kind = row.getString("kind");
                    String deprecated = row.getString("deprecated");

                    String line;
                    if ("path".equals(kind)) {
                        line = value + "<br/>";
                    } else {
                        if ("true".equals(deprecated)) {
                            line = "<s>" + name + "</s>=" + value + "<br/>";
                        } else {
                            line = name + "=" + value + "<br/>";
                        }
                    }
                    options.append("<br/>");
                    options.append("<b>").append(line).append("</b>");

                    String summary = row.getString("description");
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
        if (component.isDeprecated()) {
            sb.append("<b><s>").append(component.getTitle()).append(" Component (deprecated)</s></b><br/>");
        } else {
            sb.append("<b>").append(component.getTitle()).append(" Component</b><br/>");
        }
        sb.append(wrapText(component.getDescription(), wrapLength)).append("<br/><br/>");
        if (component.getDeprecatedSince() != null) {
            sb.append("<b>Deprecated Since:</b> <tt>").append(component.getDeprecatedSince()).append("</tt><br/>");
        }
        sb.append("<b>Since:</b> <tt>").append(component.getFirstVersionShort()).append("</tt><br/>");
        if (component.getSupportLevel() != null) {
            sb.append("<b>Support Level:</b> <tt>").append(component.getSupportLevel()).append("</tt><br/>");
        }
        String g = component.getGroupId();
        String a = component.getArtifactId();
        String v = component.getVersion();
        if (g != null && a != null && v != null) {
            sb.append("<b>Maven:</b> <tt>").append(g).append(":").append(a).append(":").append(v).append("</tt><br/>");
        }
        sb.append("<b>Syntax:</b> <tt>").append(component.getSyntax()).append("?options</tt><br/>");
        sb.append("<p/>");
        sb.append("<br/>");

        // indent the endpoint url with 5 spaces and wrap it by url separator
        String wrapped = wrapSeparator(val, "&", "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", 100);
        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>").append(wrapped).append("</b><br/>");

        if (options.length() > 0) {
            sb.append(options);
        }
        return sb.toString();
    }

    private boolean isPsiMethodCamelLanguage(PsiMethod method) {
        PsiType type = method.getReturnType();
        if (type instanceof PsiClassReferenceType) {
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

    /**
     * {@code OptionDocumentationElement} represents a PSI element containing the documentation of a given option.
     */
    static class OptionDocumentationElement extends LightElement implements DocumentationAsString {

        /**
         * The option for which the documentation is expected.
         */
        private final transient BaseOptionModel option;

        /**
         * Construct a {@code OptionDocumentationElement} with the given parameters.
         * @param manager the PSI manager
         * @param language the expected language
         * @param suggestion the holder of the option that has been suggested for which the documentation is expected.
         */
        OptionDocumentationElement(@NotNull PsiManager manager, @NotNull Language language,
                                   @NotNull OptionSuggestion suggestion) {
            super(manager, language);
            this.option = suggestion.getOption();
        }

        @Override
        public String toString() {
            return generateCamelOptionDocumentation(option);
        }

    }

    /**
     * {@code SimpleSuggestionDocumentationElement} represents a PSI element containing the documentation of a given item.
     */
    static class SimpleSuggestionDocumentationElement extends LightElement implements DocumentationAsString {

        /**
         * The suggestion for which the documentation is expected.
         */
        private final transient SimpleSuggestion suggestion;

        /**
         * Construct a {@code SimpleSuggestionDocumentationElement} with the given parameters.
         * @param manager the PSI manager
         * @param language the expected language
         * @param suggestion the suggestion for which the documentation is expected.
         */
        SimpleSuggestionDocumentationElement(@NotNull PsiManager manager, @NotNull Language language,
                                             @NotNull SimpleSuggestion suggestion) {
            super(manager, language);
            this.suggestion = suggestion;
        }

        @Override
        public String toString() {
            return generateCamelSimpleDocumentation(suggestion);
        }
    }

    /**
     * {@code DocumentationAsString} represents a type of PSI element whose documentation is accessible from the
     * {@link #toString()} method.
     */
    interface DocumentationAsString extends PsiElement {
    }
}
