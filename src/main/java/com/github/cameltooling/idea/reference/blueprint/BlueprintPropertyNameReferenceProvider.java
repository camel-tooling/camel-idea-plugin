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

import com.github.cameltooling.idea.util.BeanUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Provides references from bean property name to its backing field or setter method in the given bean
 */
public class BlueprintPropertyNameReferenceProvider extends BlueprintAttributeValueReferenceProvider {

    @Override
    protected PsiReference[] getAttributeReferences(@NotNull XmlAttribute attribute, @NotNull XmlAttributeValue value,
                                                    ProcessingContext context) {
        if (isPropertyName(attribute, value)) {
            PsiClass beanClass = BeanUtils.getService().getPropertyBeanClass(attribute.getParent());
            if (beanClass != null) {
                return new PsiReference[] {new PropertyNameReference(value, value.getValue(), beanClass)};
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private boolean isPropertyName(XmlAttribute attribute, XmlAttributeValue value) {
        return attribute.getParent().getLocalName().equals("property")
            && attribute.getLocalName().equals("name")
            && !value.getValue().isEmpty();
    }

}
