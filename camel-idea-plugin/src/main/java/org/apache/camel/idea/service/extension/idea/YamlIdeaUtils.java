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
package org.apache.camel.idea.service.extension.idea;

import java.util.Optional;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.apache.camel.idea.extension.IdeaUtilsExtension;
import org.jetbrains.annotations.NotNull;

public class YamlIdeaUtils implements IdeaUtilsExtension {
    @Override
    public Optional<String> extractTextFromElement(PsiElement element, boolean concatString, boolean stripWhitespace) {
        // maybe its yaml
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("yaml")) {
                return Optional.ofNullable(element.getText());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isElementFromSetterProperty(@NotNull PsiElement element, @NotNull String setter) {
        return false;
    }

    @Override
    public boolean isExtensionEnabled() {
        final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.plugins.yaml"));
        return plugin != null && plugin.isEnabled();
    }

}
