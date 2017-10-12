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
package org.apache.camel.idea.completion.contributor;

import com.intellij.codeInsight.completion.CompletionType;
import org.apache.camel.idea.completion.extension.CamelPropertyPlaceholderSmartCompletionExtension;
import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA completion system, to setup smart completion for Camel property placeholders (eg {{foo}} style)
 */
public class CamelPropertyPlaceholderReferenceContributor extends CamelContributor {

    public CamelPropertyPlaceholderReferenceContributor() {
        // add hook for smart completion for {{ }} placeholders
        addCompletionExtension(new CamelPropertyPlaceholderSmartCompletionExtension());
        extend(CompletionType.BASIC,
                psiElement().notNull(), // this works anywhere
                new EndpointCompletion(getCamelCompletionExtensions())
        );
    }

}
