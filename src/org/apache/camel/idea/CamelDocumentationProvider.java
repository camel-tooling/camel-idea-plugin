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
package org.apache.camel.idea;

import java.util.List;
import java.util.Map;

import com.intellij.lang.documentation.CodeDocumentationProvider;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.IdeaUtils.isStringLiteral;

/**
 * Camel documentation provider to hook into IDEA to show Camel endpoint documentation in popups and various other places.
 */
public class CamelDocumentationProvider implements CodeDocumentationProvider {

    // TODO: implement the other methods for more documentation such as F1

    private final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    @Nullable
    @Override
    public PsiComment findExistingDocComment(PsiComment contextElement) {
        return null;
    }

    @Nullable
    @Override
    public Pair<PsiElement, PsiComment> parseContext(@NotNull PsiElement startPoint) {
        return null;
    }

    @Nullable
    @Override
    public String generateDocumentationContentStub(PsiComment contextComment) {
        return null;
    }

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

                StringBuilder sb = new StringBuilder();
                if (groupId != null && artifactId != null && version != null && javaType != null) {
                    sb.append("[Maven: ").append(groupId).append(":").append(artifactId).append(":").append(version).append("] ").append(javaType).append("\n");
                }
                sb.append(title).append(": ").append(description).append("\n");
                sb.append("<b>").append(syntax).append("</b>\n");
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
}
