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
package org.apache.camel.idea.extension;

import java.util.Optional;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;


/**
 * Extension point for IdeaUtils for handling specific plugin language elements
 */
public interface IdeaUtilsExtension {

    ExtensionPointName<IdeaUtilsExtension> EP_NAME = ExtensionPointName.create("org.apache.camel.IdeaUtilsSupport");

    /**
     * Extract the text value from the {@link PsiElement} from any of the support languages this plugin works with.
     *
     * @param element the element
     * @param concatString concatenated the string if it wrapped
     * @param stripWhitespace
     * @return the text or <tt>null</tt> if the element is not a text/literal kind.
     */
    Optional<String> extractTextFromElement(@NotNull PsiElement element, boolean concatString, boolean stripWhitespace);


    /**
     * Is the element from a java setter method (eg setBrokerURL) or from a XML configured <tt>bean</tt> style
     * configuration using <tt>property</tt> element.
     */
    boolean isElementFromSetterProperty(@NotNull PsiElement element, @NotNull String setter);

    boolean isExtensionEnabled();

}
