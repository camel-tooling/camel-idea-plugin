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
package com.github.cameltooling.idea.util;

import com.github.cameltooling.idea.Constants;
import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.reference.blueprint.PropertyNameReference;
import com.github.cameltooling.idea.reference.blueprint.model.ReferenceableBeanId;
import com.github.cameltooling.idea.reference.blueprint.model.ReferencedClass;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility methods to work with beans (blueprint, spring)
 */
public class BeanUtils implements Disposable {

    public static BeanUtils getService() {
        return ApplicationManager.getApplication().getService(BeanUtils.class);
    }

    /**
     * Checks whether this element represents a declaration of a bean, e.g. a <bean> xml tag
     */
    public boolean isBeanDeclaration(PsiElement element) {
        if (element instanceof XmlTag) {
            XmlTag tag = (XmlTag) element;
            return isBlueprintNamespace(tag.getNamespace()) && tag.getLocalName().equals("bean");
        }
        return false;
    }

    private boolean isBlueprintNamespace(String namespace) {
        return namespace.contains(Constants.OSGI_BLUEPRINT_NAMESPACE);
    }

    /**
     * Finds {@link ReferenceableBeanId} for the given beanId.
     */
    public Optional<ReferenceableBeanId> findReferenceableBeanId(@NotNull Module module, @NotNull String id) {
        List<ReferenceableBeanId> results = findReferenceableIds(module, id::equals, true);
        if (results.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(results.get(0));
        }
    }

    /**
     * Finds {@link ReferenceableBeanId} that has an id that matches the given predicate
     */
    public List<ReferenceableBeanId> findReferenceableBeanIds(@NotNull Module module, @NotNull Predicate<String> idCondition) {
        return findReferenceableIds(module, idCondition, false);
    }

    private List<ReferenceableBeanId> findReferenceableIds(@NotNull Module module, Predicate<String> idCondition, boolean stopOnMatch) {
        List<ReferenceableBeanId> results = new ArrayList<>();
        final IdeaUtils ideaUtils = IdeaUtils.getService();
        ideaUtils.iterateXmlDocumentRoots(module, root -> {
            if (isPartOfBeanContainer(root)) {
                ideaUtils.iterateXmlNodes(root, XmlTag.class, tag -> Optional.of(tag)
                        .filter(this::isPartOfBeanContainer)
                        .map(contextTag -> findAttributeValue(contextTag, "id").orElse(null))
                        .filter(id -> idCondition.test(id.getValue()))
                        .map(id -> createReferenceableId(tag, id))
                        .map(ref -> {
                            results.add(ref);
                            return !stopOnMatch;
                        })
                        .orElse(true));
            }
        });
        return results;
    }

    public boolean isPartOfBeanContainer(PsiElement element) {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
        return tag != null && Arrays.stream(Constants.ACCEPTED_NAMESPACES).anyMatch(ns -> tag.getNamespace().equals(ns));
    }

    public List<ReferenceableBeanId> findReferenceableBeanIdsByType(Module module, PsiType expectedBeanType) {
        return findReferenceableBeanIdsByType(module, id -> true, expectedBeanType);
    }

    public List<ReferenceableBeanId> findReferenceableBeanIdsByType(Module module,
                                                                    Predicate<String> idCondition,
                                                                    PsiType expectedBeanType) {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(module.getProject());
        return findReferenceableBeanIds(module, idCondition).stream()
                .filter(ref -> {
                    PsiClass psiClass = resolveToPsiClass(ref);
                    if (psiClass != null) {
                        PsiClassType beanType = elementFactory.createType(psiClass);
                        return expectedBeanType.isAssignableFrom(beanType);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public Optional<PsiClass> findReferencedBeanClass(PsiElement element) {
        return findBeanReference(element)
                .map(this::resolveToPsiClass);
    }

    private PsiClass resolveToPsiClass(ReferenceableBeanId ref) {
        return (PsiClass) Optional.of(ref)
                .map(ReferenceableBeanId::getReferencedClass)
                .map(ReferencedClass::getReference)
                .map(PsiReference::resolve)
                .filter(e -> e instanceof PsiClass)
                .orElse(null);
    }

    private Optional<ReferenceableBeanId> findBeanReference(PsiElement element) {
        PsiReference[] references = element.getReferences();
        return Arrays.stream(references)
                .filter(r -> r instanceof BeanReference)
                .map(r -> ((BeanReference) r).findReferenceableBeanId().orElse(null))
                .filter(Objects::nonNull)
                .findAny();
    }

    private Optional<XmlAttributeValue> findAttributeValue(XmlTag tag, String localName) {
        return IdeaUtils.getService().findAttributeValue(tag, localName);
    }

    private ReferenceableBeanId createReferenceableId(@NotNull XmlTag tag, @NotNull XmlAttributeValue idValue) {
        ReferencedClass referencedClass = findAttributeValue(tag, "class")
                .map(this::findReferencedClass)
                .orElse(null);
        return new ReferenceableBeanId(idValue, idValue.getValue(), referencedClass);
    }

    private ReferencedClass findReferencedClass(XmlAttributeValue element) {
        JavaClassReference ref = JavaClassUtils.getService().findClassReference(element);
        if (ref != null) {
            return new ReferencedClass(element.getValue(), ref);
        }
        return null;
    }

    /**
     * If element at this location specifies a reference to a bean (e.g. a BeanInject annotation, bean property reference,
     * etc.), returns the expected type of that bean
     */
    public PsiType findExpectedBeanTypeAt(PsiElement location) {
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(location, PsiAnnotation.class);
        if (annotation != null) {
            return IdeaUtils.getService().findAnnotatedElementType(annotation);
        }
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(location, XmlAttributeValue.class, false))
                .filter(e -> Arrays.stream(e.getReferences()).anyMatch(ref -> ref instanceof BeanReference))
                .map(e -> PsiTreeUtil.getParentOfType(e, XmlTag.class))
                .map(this::findPropertyNameReference)
                .map(PropertyNameReference::resolve)
                .map(target -> {
                    if (target instanceof PsiField) {
                        return ((PsiField) target).getType();
                    } else if (target instanceof PsiMethod) {
                        return ((PsiMethod) target).getParameterList().getParameters()[0].getType();
                    } else {
                        return null;
                    }
                })
                .orElse(null);
    }

    private PropertyNameReference findPropertyNameReference(XmlTag propertyTag) {
        return IdeaUtils.getService().findAttributeValue(propertyTag, "name")
                .map(a -> {
                    PsiReference ref = a.getReference();
                    return ref instanceof PropertyNameReference ? (PropertyNameReference) ref : null;
                }).orElse(null);
    }

    public PsiClass getPropertyBeanClass(XmlTag propertyTag) {
        XmlTag beanTag = propertyTag.getParentTag();
        if (beanTag != null && isBeanDeclaration(beanTag)) {
            IdeaUtils ideaUtils = IdeaUtils.getService();
            JavaClassUtils javaClassUtils = JavaClassUtils.getService();
            return ideaUtils.findAttributeValue(beanTag, "class")
                    .map(javaClassUtils::findClassReference)
                    .map(javaClassUtils::resolveClassReference)
                    .orElse(null);
        }
        return null;
    }

    @Override
    public void dispose() {

    }

}
