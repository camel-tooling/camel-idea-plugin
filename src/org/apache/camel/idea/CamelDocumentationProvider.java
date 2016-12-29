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

import java.util.List;
import java.util.Map;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.IdeaUtils.isStringLiteral;
import static org.apache.camel.idea.StringUtils.asLanguageName;

/**
 * Camel documentation provider to hook into IDEA to show Camel endpoint documentation in popups and various other places.
 */
public class CamelDocumentationProvider implements DocumentationProvider {

    private final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (isStringLiteral(originalElement)) {
            PsiLiteralExpression exp = (PsiLiteralExpression) originalElement;
            String val = (String) exp.getValue();
            String componentName = StringUtils.asComponentName(val);
            if (componentName != null && camelCatalog.findComponentNames().contains(componentName)) {
                // it is a known Camel component
                String json = camelCatalog.componentJSonSchema(componentName);

                List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("component", json, false);

                String title = "";
                String description = "";
                String syntax = "";
                String groupId = null;
                String artifactId = null;
                String version = null;
                String javaType = null;
                for (Map<String, String> row : rows) {
                    if (row.containsKey("title")) {
                        title = row.get("title");
                    }
                    if (row.containsKey("description")) {
                        description = row.get("description");
                    }
                    if (row.containsKey("syntax")) {
                        syntax = row.get("syntax");
                    }
                    if (row.containsKey("groupId")) {
                        groupId = row.get("groupId");
                    }
                    if (row.containsKey("artifactId")) {
                        artifactId = row.get("artifactId");
                    }
                    if (row.containsKey("version")) {
                        version = row.get("version");
                    }
                    if (row.containsKey("javaType")) {
                        javaType = row.get("javaType");
                    }
                }

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
                if (groupId != null && artifactId != null && version != null && javaType != null) {
                    sb.append("[Maven: ").append(groupId).append(":").append(artifactId).append(":").append(version).append("] ").append(javaType).append("\n");
                }
                sb.append("\n");
                sb.append("<b>").append(title).append("</b>: ").append(syntax).append("\n");
                sb.append(description).append("\n");

                sb.append("\n");
                // must wrap val as IDEA cannot handle very big lines
                String wrapped = WordUtils.wrap(val, 120);
                sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>").append(wrapped).append("</b>\n");

                if (options.length() > 0) {
                    sb.append(options.toString());
                }
                return sb.toString();
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
        if (isStringLiteral(element)) {
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            String val = (String) literal.getValue();
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
