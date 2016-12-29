/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.intellij.ide.BrowserUtil;
import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.lang.documentation.ExternalDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.ModelHelper;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.IdeaUtils.isStringLiteral;
import static org.apache.camel.idea.StringUtils.asComponentName;

/**
 * Camel documentation provider to hook into IDEA to show Camel endpoint documentation in popups and various other places.
 */
public class CamelDocumentationProvider extends DocumentationProviderEx implements ExternalDocumentationProvider, ExternalDocumentationHandler {

    private final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    //                                      https://github.com/apache/camel/blob/master/camel-core/src/main/docs/timer-component.adoc
    private final String externalBaseUrl = "https://github.com/apache/camel/blob/master";

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        String val = generateDocFor(element);
        if (val == null) {
            return null;
        }

        String componentName = StringUtils.asComponentName(val);
        if (componentName != null && camelCatalog.findComponentNames().contains(componentName)) {

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
                            line = value + "\n";
                        } else {
                            line = name + "=" + value + "\n";
                        }
                        options.append("\n");
                        options.append("<b>").append(line).append("</b>");

                        String summary = row.get("description");
                        // must wrap summary as IDEA cannot handle very big lines
                        String wrapped = WordUtils.wrap(summary, 120);
                        options.append(wrapped).append("\n");
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            String g = component.getGroupId();
            String a = component.getArtifactId();
            String v = component.getVersion();
            if (g != null && a != null && v != null) {
                sb.append("[Maven: ").append(g).append(":").append(a).append(":").append(v).append("] ");
                if (component.getJavaType() != null) {
                    sb.append(component.getJavaType());
                }
                sb.append("\n");
            }
            sb.append("\n");
            sb.append("<b>").append(component.getTitle()).append("</b>: ").append(component.getSyntax()).append("\n");
            sb.append(component.getDescription()).append("\n");

            sb.append("\n");
            // must wrap val as IDEA cannot handle very big lines
            String wrapped = WordUtils.wrap(val, 120);
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>").append(wrapped).append("</b>\n");

            if (options.length() > 0) {
                sb.append(options.toString());
            }
            return sb.toString();
        }

        return null;
    }

    @Nullable
    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        return null;
        /*String text = generateDocFor(element);
        if (text != null) {
            // check if its a known Camel component
            String name = asComponentName(text);
            if (camelCatalog.findComponentNames().contains(name)) {
                List<String> list = new ArrayList<>();
                String json = camelCatalog.componentJSonSchema(name);
                ComponentModel component = ModelHelper.generateComponentModel(json, false);

                // to build external links which points to github
                String a = component.getArtifactId();

                String url;
                if ("camel-core".equals(a)) {
                    url = externalBaseUrl + "/camel-core/src/main/docs/" + name + "-component.adoc";
                } else {
                    url = externalBaseUrl + "/components/" + component.getArtifactId() + "/src/main/docs/" + name + "-component.adoc";
                }
                list.add(url);
                return list;
            }
        }

        return null;*/
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        String doc = getQuickNavigateInfo(element, originalElement);
        return doc;

        /*
        String val = generateDocFor(element);
        if (val == null) {
            return null;
        }

        String componentName = StringUtils.asComponentName(val);
        if (componentName != null) {
            return camelCatalog.componentHtmlDoc(componentName);
        } else {
            // its maybe a method call for a Camel language
            // which we need to try find out using IDEA Psi which can be cumbersome and complex
            PsiElement parent = element.getParent();
            if (parent instanceof PsiExpressionList) {
                parent = parent.getParent();
            }
            if (parent instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression call = (PsiMethodCallExpression) parent;
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

        return null;*/
    }

    private String generateDocFor(PsiElement element) {
        if (isStringLiteral(element)) {
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            return (String) literal.getValue();
        }

        // its maybe a property from properties file
        String fqn = element.getClass().getName();
        if (fqn.startsWith("com.intellij.lang.properties.psi.impl.PropertyValue")) {
            // yes we can support this also
            return element.getText();
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
        if (hasDocumentationForCamelComponent(contextElement)) {
            return contextElement;
        }
        return null;
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

    @Nullable
    @Override
    public String fetchExternalDocumentation(Project project, PsiElement element, List<String> docUrls) {
        if (docUrls.size() == 1) {
            String link = docUrls.get(0);
            if (link.startsWith(externalBaseUrl) && link.endsWith("-component.adoc")) {
                // grab name from url
                int pos = link.lastIndexOf("/");
                String name = link.substring(pos + 1, link.length() - 15);
                return camelCatalog.componentHtmlDoc(name);
            }
        }
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

    private boolean hasDocumentationForCamelComponent(PsiElement element) {
        String text = generateDocFor(element);
        if (text != null) {
            // check if its a known Camel component
            String name = asComponentName(text);
            return camelCatalog.findComponentNames().contains(name);
        }
        return false;
    }

    @Override
    public boolean handleExternal(PsiElement element, PsiElement originalElement) {
        String val = generateDocFor(element);
        if (val == null) {
            return false;
        }

        String name = StringUtils.asComponentName(val);
        if (name != null && camelCatalog.findComponentNames().contains(name)) {

            String json = camelCatalog.componentJSonSchema(name);
            ComponentModel component = ModelHelper.generateComponentModel(json, false);

            // to build external links which points to github
            String a = component.getArtifactId();

            String url;
            if ("camel-core".equals(a)) {
                url = externalBaseUrl + "/camel-core/src/main/docs/" + name + "-component.adoc";
            } else {
                url = externalBaseUrl + "/components/" + component.getArtifactId() + "/src/main/docs/" + name + "-component.adoc";
            }

            String hash = component.getTitle().toLowerCase().replace(' ', '-') + "-component";
            BrowserUtil.browse(url + "#" + hash);
            return true;
        }

        return false;
    }

    @Override
    public boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context) {
        return link.startsWith(externalBaseUrl) && link.endsWith("-component.adoc");
    }

    @Override
    public boolean canFetchDocumentationLink(String link) {
        return link.startsWith(externalBaseUrl) && link.endsWith("-component.adoc");
    }

    @NotNull
    @Override
    public String fetchExternalDocumentation(@NotNull String link, @Nullable PsiElement element) {
        if (link.startsWith(externalBaseUrl) && link.endsWith("-component.adoc")) {
            // grab name from url
            int pos = externalBaseUrl.lastIndexOf("/");
            String name = externalBaseUrl.substring(pos, externalBaseUrl.length() - 15);
            return camelCatalog.componentHtmlDoc(name);
        }
        return null;
    }
}
