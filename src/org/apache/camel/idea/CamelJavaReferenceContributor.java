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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestions;
import static org.apache.camel.idea.CamelSmartCompletionEndpointValue.addSmartCompletionForSingleValue;

/**
 * Plugin to hook into the IDEA Java language, to setup Camel smart completion for editing Java source code.
 */
public class CamelJavaReferenceContributor extends PsiReferenceContributor {

    private final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // register java based reference registrar which allows this plugin to hook into IDEA's smart completion / suggestion
        // where we can then discover Camel endpoints and provide a list of suggested values
        registrar.registerReferenceProvider(getElementPattern(), new PsiReferenceProvider() {
            @NotNull
            public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
                return new CamelEndpointLiteralReference[]{new CamelEndpointLiteralReference((PsiLiteral) element, camelCatalog)};
            }
        });
    }

    private static PsiElementPattern.Capture<PsiLiteral> getElementPattern() {
        return PlatformPatterns.psiElement(PsiLiteral.class).and(new FilterPattern(new StringLiteralFilter()));
    }

    /**
     * Allows to provide smart completions for literals
     */
    private static class CamelEndpointLiteralReference extends PsiReferenceBase<PsiLiteral> {

        private final CamelCatalog camelCatalog;

        public CamelEndpointLiteralReference(PsiLiteral element, CamelCatalog camelCatalog) {
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
            List<Object> answer = null;

            // special IDEA hack which is really needed (yes they do this)
            String val = getValue();
            int hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER);
            if (hackIndex == -1) {
                hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED);
            }
            if (hackIndex > -1) {
                val = val.substring(0, hackIndex);
            }

            // is this a possible Camel endpoint uri which we know
            String componentName = StringUtils.asComponentName(val);
            if (componentName != null && camelCatalog.findComponentNames().contains(componentName)) {

                // it is a known Camel component
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
                    answer = addSmartCompletionForSingleValue(val, rows, name);
                } else {
                    answer = addSmartCompletionSuggestions(val, rows, existing);
                }
            }
            if (answer != null) {
                return answer.toArray();
            } else {
                return Collections.emptyList().toArray();
            }
        }
    }

}
