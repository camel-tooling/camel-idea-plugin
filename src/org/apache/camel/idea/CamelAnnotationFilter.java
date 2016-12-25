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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * {@link ElementFilter} to discover Camel annotations.
 */
class CamelAnnotationFilter implements ElementFilter {

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
