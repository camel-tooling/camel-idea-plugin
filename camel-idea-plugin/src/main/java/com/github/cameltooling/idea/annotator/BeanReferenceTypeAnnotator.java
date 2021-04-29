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
package com.github.cameltooling.idea.annotator;

import java.util.Arrays;
import java.util.Optional;
import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.util.BeanUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Finds and validates whether a bean reference has the correct type which is expected at the location of the usage.
 * For example, if a we have a declaration like this:
 * <pre>
 * {@code
 *   <bean id='myBean' class='org.foo.MyBean'>
 *       <property name='xxx' ref='stringBean'/>
 *   </bean>
 * }
 * </pre>
 * The 'stringBean' bean is a reference to a bean declaration with {@link String} type, this validates if the
 * type of the property 'xxx' in bean 'org.foo.MyBean' really should be a {@link String}.
 */
public class BeanReferenceTypeAnnotator extends AbstractCamelAnnotator {

    @Override
    boolean isEnabled() {
        return CamelPreferenceService.getService().isRealTimeIdReferenceTypeValidation();
    }

    @Override
    boolean accept(PsiElement element) {
        if (!super.accept(element)) {
            return false;
        }

        if (element != null) {
            return isBeanReference(element) || isBeanPropertyReference(element);
        }
        return false;
    }

    @Override
    void validateText(@NotNull PsiElement element, @NotNull AnnotationHolder holder, @NotNull String text) {
        PsiClass beanClass = BeanUtils.getService().findReferencedBeanClass(element).orElse(null);
        TextRange elementRange = IdeaUtils.getService().getUnquotedRange(element);
        PsiType expectedType = BeanUtils.getService().findExpectedBeanTypeAt(element);
        if (expectedType != null) {
            if (beanClass == null) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Unable to determine type of the referenced bean.")
                        .range(elementRange).create();
            } else if (!isAssignableFrom(expectedType, beanClass)) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Bean must be of '" + expectedType.getCanonicalText() + "' type")
                        .range(elementRange).create();
            }
        }
    }

    private boolean isAssignableFrom(PsiType type, PsiClass value) {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(value.getProject());
        PsiClassType valueType = elementFactory.createType(value);
        return type.isAssignableFrom(valueType);
    }

    private boolean isBeanReference(PsiElement element) {
        return Arrays.stream(element.getReferences()).anyMatch(ref -> ref instanceof BeanReference);
    }

    private boolean isBeanPropertyReference(PsiElement element) {
        Optional<XmlTag> parentTag = Optional.ofNullable(PsiTreeUtil.getParentOfType(element, XmlTag.class));
        return isCamelXmlAttributeValue(element)
            && parentTag.filter(parent -> parent.getLocalName().equals("property")).isPresent();
    }

    private boolean isCamelXmlAttributeValue(PsiElement element) {
        return element instanceof XmlAttributeValue && BeanUtils.getService().isPartOfBeanContainer(element);
    }

}
