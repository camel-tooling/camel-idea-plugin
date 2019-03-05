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
package org.apache.camel.idea.reference.endpoint;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.reference.CamelPsiReferenceProvider;
import org.apache.camel.idea.util.CamelIdeaUtils;

public abstract class CamelEndpointPsiReferenceProvider extends CamelPsiReferenceProvider {

    @Override
    protected PsiReference[] getCamelReferencesByElement(PsiElement element, ProcessingContext context) {
        String endpointUri = getEndpointUri(element);
        if (endpointUri == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        if (!isEndpoint(endpointUri)) {
            return PsiReference.EMPTY_ARRAY;
        }
        if (!CamelIdeaUtils.getService().isInsideCamelRoute(element, false)) {
            return PsiReference.EMPTY_ARRAY;
        }
        return getEndpointReferencesByElement(endpointUri, element, context);
    }

    protected abstract PsiReference[] getEndpointReferencesByElement(String endpointUri, PsiElement element,
                                                                     ProcessingContext context);

    protected abstract boolean isEndpoint(String endpointUri);

    private String getEndpointUri(PsiElement element) {
        if (element instanceof PsiLiteralValue) {
            PsiLiteralValue valueElement = (PsiLiteralValue) element;
            Object value = valueElement.getValue();
            if (value instanceof String) {
                return (String) value;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
