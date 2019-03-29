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
package com.github.cameltooling.idea.reference.blueprint;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Provides references from blueprint xml attributes to java classes, e.g. the 'class' attribute of a bean element.
 */
public class BlueprintJavaClassReferenceProvider extends BlueprintAttributeValueReferenceProvider {

    private final JavaClassReferenceProvider javaClassReferenceProvider;

    public BlueprintJavaClassReferenceProvider() {
        this.javaClassReferenceProvider = new JavaClassReferenceProvider();
    }

    @Override
    protected PsiReference[] getAttributeReferences(@NotNull XmlAttribute attribute, @NotNull XmlAttributeValue value,
                                                    ProcessingContext context) {
        if (isBeanClassAttribute(attribute) || isTypeAttributeInRoute(attribute)) {
            return javaClassReferenceProvider.getReferencesByElement(value, context);
        } else {
            return PsiReference.EMPTY_ARRAY;
        }
    }

    private boolean isTypeAttributeInRoute(XmlAttribute attribute) {
        return false;
    }

    private boolean isBeanClassAttribute(XmlAttribute attribute) {
        return CamelIdeaUtils.getService().isBeanDeclaration(attribute.getParent()) && attribute.getLocalName().equals("class");
    }

}