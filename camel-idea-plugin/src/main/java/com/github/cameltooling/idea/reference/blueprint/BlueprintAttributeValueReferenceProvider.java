/*
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
package com.github.cameltooling.idea.reference.blueprint;

import com.github.cameltooling.idea.reference.CamelPsiReferenceProvider;
import com.github.cameltooling.idea.util.BeanUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Parent for reference providers that provide references from xml attribute values in blueprint files
 */
public abstract class BlueprintAttributeValueReferenceProvider extends CamelPsiReferenceProvider {

    @Override
    protected PsiReference[] getCamelReferencesByElement(PsiElement element, ProcessingContext context) {
        if (!(element instanceof XmlAttributeValue)) {
            return PsiReference.EMPTY_ARRAY;
        }

        XmlAttribute attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute.class);
        if (attribute == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        XmlTag tag = attribute.getParent();
        XmlAttributeValue value = attribute.getValueElement();
        if (tag == null || value == null || !BeanUtils.getService().isPartOfBeanContainer(tag)) {
            return PsiReference.EMPTY_ARRAY;
        }

        return getAttributeReferences(attribute, value, context);
    }

    protected abstract PsiReference[] getAttributeReferences(@NotNull XmlAttribute attribute,
                                                             @NotNull XmlAttributeValue value,
                                                             ProcessingContext context);

}
