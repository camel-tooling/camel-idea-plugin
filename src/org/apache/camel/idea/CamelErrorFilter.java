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

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class CamelErrorFilter extends HighlightErrorFilter {

    @Override
    public boolean shouldHighlightErrorElement(@NotNull final PsiErrorElement element) {
        PsiDocComment doc = value(element);
        if (doc != null && !doc.getText().contains("Camel")) {
            return true;
        }
        return false;
    }

    public static PsiDocComment value(final PsiErrorElement element) {
        PsiDocComment doc = PsiTreeUtil.getParentOfType(element, PsiDocComment.class);
        return doc;
    }
}
