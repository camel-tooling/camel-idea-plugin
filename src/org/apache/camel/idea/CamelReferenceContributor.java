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

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelReferenceContributor extends PsiReferenceContributor {

    private final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    private static PsiElementPattern.Capture<PsiLiteral> getElementPattern() {
        return PlatformPatterns.psiElement(PsiLiteral.class).and(new FilterPattern(new CamelAnnotationFilter()));
    }

    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(getElementPattern(), new PsiReferenceProvider() {
            @NotNull
            public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
                return new MethodReference[]{new MethodReference((PsiLiteral) element, camelCatalog)};
            }
        });
    }

    private static class MethodReference extends PsiReferenceBase<PsiLiteral> {

        private final CamelCatalog camelCatalog;

        public MethodReference(PsiLiteral element, CamelCatalog camelCatalog) {
            super(element, false);
            this.camelCatalog = camelCatalog;
        }

        @Override
        public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
            if (element instanceof PsiMethod) {
                return handleElementRename(((PsiMethod) element).getName());
            }
            return super.bindToElement(element);
        }

        @Nullable
        public PsiElement resolve() {
            return getElement();
        }

        @NotNull
        public Object[] getVariants() {
            List<Object> list = new ArrayList<>();

            @NonNls String val = getValue();
            int hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER);
            if (hackIndex > -1) {
                val = val.substring(0, hackIndex);
            }

            // is it after ? mark as we only do completion until
            // is it a Camel component
            if (hasQuestionMark(val)) {
                String componentName = asComponentName(val);
                if (componentName != null && camelCatalog.findComponentNames().contains(componentName)) {
                    // is it a known Camel component
                    String json = camelCatalog.componentJSonSchema(componentName);

                    // gather list of names
                    List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("properties", json, true);

                    // grab all existing parameters
                    String query = val;
                    // strip up ending incomplete parameter
                    if (query.endsWith("&") || query.endsWith("?")) {
                        query = query.substring(0, query.length() - 1);
                    }

                    Map<String, String> existing = null;
                    try {
                        existing = camelCatalog.endpointProperties(query);
                    } catch (Exception e) {
                        // ignore
                    }

                    // are we editing an existing parameter value
                    // or are we having a list of suggested parameters to choose among

                    boolean editSingle = val.endsWith("=");
                    if (editSingle) {
                        // parameter name is before = and & or ?
                        int pos = Math.max(val.lastIndexOf('&'), val.lastIndexOf('?'));
                        String name = val.substring(pos + 1);
                        name = name.substring(0, name.length() - 1); // remove =
                        addSmartCompletionForSingleValue(list, val, rows, name);
                    } else {
                        addSmartCompletionSuggestions(list, val, rows, existing);
                    }
                }
            }
            return list.toArray();
        }

        private void addSmartCompletionForSingleValue(List<Object> list, String val, List<Map<String, String>> rows, String name) {
            Map<String, String> found = null;
            for (Map<String, String> row : rows) {
                if (name.equals(row.get("name"))) {
                    found = row;
                    break;
                }
            }
            if (found != null) {
                String javaType = found.get("javaType");
                String deprecated = found.get("deprecated");
                String enums = found.get("enum");
                String defaultValue = found.get("defaultValue");

                if (enums != null) {
                    String[] parts = enums.split(",");
                    for (String part : parts) {
                        String lookup = val + part;
                        LookupElementBuilder builder = LookupElementBuilder.create(lookup);
                        // only show the option in the UI
                        builder = builder.withPresentableText(part);
                        builder = builder.withBoldness(true);
                        if ("true".equals(deprecated)) {
                            // mark as deprecated
                            builder = builder.withStrikeoutness(true);
                        }
                        boolean isDefaultValue = defaultValue != null && part.equals(defaultValue);
                        if (isDefaultValue) {
                            builder = builder.withTailText(" (default value)");
                            // add default value first in the list
                            list.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                        } else {
                            list.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                        }
                    }
                } else if ("java.lang.Boolean".equals(javaType) || "boolean".equals(javaType)) {
                    // for boolean types then give a choice between true|false
                    String lookup = val + "true";
                    LookupElementBuilder builder = LookupElementBuilder.create(lookup);
                    // only show the option in the UI
                    builder = builder.withPresentableText("true");
                    if ("true".equals(deprecated)) {
                        // mark as deprecated
                        builder = builder.withStrikeoutness(true);
                    }
                    boolean isDefaultValue = defaultValue != null && "true".equals(defaultValue);
                    if (isDefaultValue) {
                        builder = builder.withTailText(" (default value)");
                        // add default value first in the list
                        list.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                    } else {
                        list.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                    }

                    lookup = val + "false";
                    builder = LookupElementBuilder.create(lookup);
                    // only show the option in the UI
                    builder = builder.withPresentableText("false");
                    if ("true".equals(deprecated)) {
                        // mark as deprecated
                        builder = builder.withStrikeoutness(true);
                    }
                    isDefaultValue = defaultValue != null && "false".equals(defaultValue);
                    if (isDefaultValue) {
                        builder = builder.withTailText(" (default value)");
                        // add default value first in the list
                        list.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                    } else {
                        list.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                    }
                }
            }
        }

        private void addSmartCompletionSuggestions(List<Object> list, String val, List<Map<String, String>> rows, Map<String, String> existing) {
            for (Map<String, String> row : rows) {
                String name = row.get("name");
                // should be uri parameters
                String kind = row.get("kind");
                String deprecated = row.get("deprecated");
                String group = row.get("group");
                String javaType = row.get("javaType");
                if ("parameter".equals(kind)) {
                    // only add if not already used
                    if (existing == null || !existing.containsKey(name)) {
                        // the lookup should prepare for the new option
                        String lookup;
                        if (existing == null) {
                            // none existing options so we need to start with a ? mark
                            lookup = val + "?" + name + "=";
                        } else {
                            lookup = val + "&" + name + "=";
                        }
                        LookupElementBuilder builder = LookupElementBuilder.create(lookup);
                        // only show the option in the UI
                        builder = builder.withPresentableText(name);
                        // we don't want to highlight the advanced options which should be more seldom in use
                        boolean advanced = group != null && group.contains("advanced");
                        builder = builder.withBoldness(!advanced);
                        if (javaType != null) {
                            builder = builder.withTypeText(javaType, true);
                        }
                        if ("true".equals(deprecated)) {
                            // mark as deprecated
                            builder = builder.withStrikeoutness(true);
                        }
                        // TODO: we could nice with an icon for producer vs consumer etc or for secret, or required
                        list.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
                    }
                }
            }
        }
    }

    private static boolean hasQuestionMark(String val) {
        return val.indexOf('?') > 0;
    }

    private static String asComponentName(String val) {
        int pos = val.indexOf(':');
        if (pos > 0) {
            return val.substring(0, pos);
        }
        return null;
    }

    private static class CamelAnnotationFilter implements ElementFilter {

        public CamelAnnotationFilter() {
        }

        public boolean isAcceptable(Object element, PsiElement context) {
            PsiNameValuePair pair = PsiTreeUtil.getParentOfType(context, PsiNameValuePair.class, false, PsiMember.class, PsiStatement.class);
            if (null == pair) return false;
            PsiAnnotation annotation = PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
            if (annotation == null) return false;
            String fqn = annotation.getQualifiedName();
            return fqn != null && fqn.startsWith("org.apache.camel");
        }

        public boolean isClassAcceptable(Class hintClass) {
            return PsiLiteral.class.isAssignableFrom(hintClass);
        }
    }
}
