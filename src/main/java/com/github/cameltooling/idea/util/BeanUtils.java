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
import com.github.cameltooling.idea.reference.blueprint.model.FactoryBeanMethodReference;
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
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
        if (element instanceof XmlTag tag) {
            return BlueprintUtils.getService().isBlueprintNamespace(tag.getNamespace()) && tag.getLocalName().equals("bean");
        }
        return false;
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
        ideaUtils.iterateXmlDocumentRoots(module.getProject(), module.getModuleContentScope(), root -> {
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
                    PsiClass psiClass = getReferencedClass(ref);
                    if (psiClass != null) {
                        PsiClassType beanType = elementFactory.createType(psiClass);
                        return expectedBeanType.isAssignableFrom(beanType);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public Optional<PsiClass> findReferencedBeanClass(PsiElement element) {
        return findReferencedBeanClass(element, new HashSet<>());
    }

    public Optional<PsiClass> findReferencedBeanClass(PsiElement element, Set<String> usedFactoryBeans) {
        return findBeanReference(element)
                .map(ref -> getReferencedClass(ref, usedFactoryBeans));
    }

    public @Nullable PsiClass getReferencedClass(@NotNull ReferenceableBeanId bean) {
        return getReferencedClass(bean, new HashSet<>());
    }

    public @Nullable PsiClass getReferencedClass(@NotNull ReferenceableBeanId bean, Set<String> usedFactoryBeans) {
        XmlTag beanTag = bean.getBeanTag();
        Optional<XmlAttributeValue> factoryMethod = findAttributeValue(beanTag, "factory-method");
        if (factoryMethod.isPresent()) {
            XmlAttributeValue methodValue = factoryMethod.get();
            FactoryBeanMethodReference methodRef = Arrays.stream(methodValue.getReferences())
                    .filter(ref -> ref instanceof FactoryBeanMethodReference)
                    .map(ref -> (FactoryBeanMethodReference) ref)
                    .findFirst()
                    .orElse(null);
            if (methodRef == null) {
                return null;
            }

            return Arrays.stream(methodRef.multiResolve(false, usedFactoryBeans))
                    .map(ResolveResult::getElement)
                    .filter(r -> r instanceof PsiMethod)
                    .map(m -> (PsiMethod) m)
                    .map(PsiMethod::getReturnType)
                    .distinct()
                    .map(PsiUtil::resolveClassInClassTypeOnly)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } else {
            ReferencedClass referencedClass = findAttributeValue(beanTag, "class")
                    .map(this::findReferencedClass)
                    .orElse(null);
            if (referencedClass != null) {
                PsiElement resolvedClass = referencedClass.getReference().resolve();
                if (resolvedClass instanceof PsiClass psiClass) {
                    return psiClass;
                }
            }
            return null;
        }
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
        return new ReferenceableBeanId(tag, idValue);
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
        return getBeanClass(beanTag);
    }

    public PsiClass getBeanClass(XmlTag beanTag) {
        if (beanTag != null && isBeanDeclaration(beanTag)) {
            IdeaUtils ideaUtils = IdeaUtils.getService();
            JavaClassUtils javaClassUtils = JavaClassUtils.getService();
            return ideaUtils.findAttributeValue(beanTag, "class")
                    .map(javaClassUtils::findClassReference)
                    .map(javaClassUtils::resolveClassReference)
                    .orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Filters a list of methods, returning only those that can be used as factory methods to create
     * the specified bean, according to its argument types.
     *
     * @param beanTag the XML tag representing the bean whose argument types are to be matched
     * @param methods the list of possible factory methods to check
     */
    public List<PsiMethod> filterPossibleBeanFactoryMethods(XmlTag beanTag, List<PsiMethod> methods) {
        PsiType[] beanArguments = getBeanArguments(beanTag);
        return methods.stream()
                .filter(m -> areArgumentsAssignable(m, beanArguments))
                .toList();
    }

    /**
     * Finds types of arguments for a bean declaration.
     * If the type can't be determined, null is returned.
     */
    private PsiType[] getBeanArguments(XmlTag beanTag) {
        if (beanTag != null && isBeanDeclaration(beanTag)) {
            return Arrays.stream(beanTag.getChildren())
                    .filter(child -> child instanceof XmlTag t && t.getLocalName().equals("argument"))
                    .map(t -> getArgumentType((XmlTag) t))
                    .toArray(PsiType[]::new);
        } else {
            return PsiType.EMPTY_ARRAY;
        }
    }

    private PsiType getArgumentType(@NotNull XmlTag argument) {
        XmlAttribute typeAttr = argument.getAttribute("type");
        if (typeAttr != null && typeAttr.getValue() != null) {
            try {
                return JavaPsiFacade.getElementFactory(argument.getProject()).createTypeFromText(typeAttr.getValue(), argument);
            } catch (IncorrectOperationException e) {
                return null;
            }
        } else if (argument.getAttribute("value") != null) {
            return null; // can't determine exact type, let's be lenient
        } else {
            XmlAttribute refAttr = argument.getAttribute("ref");
            if (refAttr != null && refAttr.getValueElement() != null) {
                return findBeanReference(refAttr.getValueElement())
                        .map(this::getReferencedClass)
                        .map(cls -> PsiElementFactory.getInstance(cls.getProject()).createType(cls))
                        .orElse(null);
            } else {
                XmlTag argBean = Arrays.stream(argument.getSubTags())
                        .filter(subTag -> subTag.getLocalName().equals("bean"))
                        .findFirst()
                        .orElse(null);
                if (argBean != null) {
                    PsiClass beanClass = getBeanClass(argBean);
                    if (beanClass != null) {
                        PsiElementFactory instance = PsiElementFactory.getInstance(argBean.getProject());
                        return instance.createType(beanClass);
                    }
                }
            }
        }
        return null;
    }

    public List<PsiMethod> findFactoryMethods(@NotNull XmlTag beanTag, @Nullable String methodName) throws FactoryBeanMethodCycleException {
        return findFactoryMethods(beanTag, methodName, new HashSet<>());
    }

    public List<PsiMethod> findFactoryMethods(@NotNull XmlTag beanTag, @Nullable String methodName, Set<String> usedFactoryBeans) throws FactoryBeanMethodCycleException {
        XmlAttribute factoryRefAttr = Arrays.stream(beanTag.getAttributes()).filter(a -> a.getLocalName().equals("factory-ref")).findFirst().orElse(null);

        if (factoryRefAttr == null) {
            PsiClass beanClass = getBeanClass(beanTag);
            if (beanClass == null) {
                return List.of();
            }
            PsiMethod[] methods = findMethodsByName(beanClass, methodName);
            return filterMatchingMethods(methods, true);
        } else {
            XmlAttributeValue factoryRefVal = factoryRefAttr.getValueElement();
            if (factoryRefVal == null) {
                return List.of();
            }
            String factoryRef = factoryRefVal.getValue();
            if (usedFactoryBeans.contains(factoryRef)) {
                throw new FactoryBeanMethodCycleException();
            }
            usedFactoryBeans.add(factoryRef);
            Optional<PsiClass> factoryClass = findReferencedBeanClass(factoryRefVal, usedFactoryBeans);
            if (factoryClass.isPresent()) {
                PsiMethod[] methods = findMethodsByName(factoryClass.get(), methodName);
                return filterMatchingMethods(methods, false);
            } else {
                return List.of();
            }
        }
    }

    private static PsiMethod[] findMethodsByName(PsiClass beanClass, @Nullable String methodName) {
        return methodName == null ? beanClass.getAllMethods() : beanClass.findMethodsByName(methodName, true);
    }

    private List<PsiMethod> filterMatchingMethods(PsiMethod[] methods, boolean staticMethods) {
        return Arrays.stream(methods)
                .filter(m -> {
                    PsiClass cc = m.getContainingClass();
                    return cc == null || !Object.class.getName().equals(cc.getQualifiedName());
                })
                .filter(m -> {
                    boolean isStatic = m.getModifierList().hasModifierProperty(PsiModifier.STATIC);
                    return staticMethods == isStatic;
                })
                .filter(m -> !m.getModifierList().hasModifierProperty(PsiModifier.PRIVATE))
                .filter(m -> !PsiTypes.voidType().equals(m.getReturnType()))
                .toList();
    }


    private boolean areArgumentsAssignable(PsiMethod method, PsiType[] argTypes) {
        PsiParameter[] params = method.getParameterList().getParameters();
        if (params.length != argTypes.length) {
            return false;
        }
        for (int i = 0; i < params.length; i++) {
            PsiType argType = argTypes[i];
            if (argType == null) {
                continue; //our marker for unknown type
            }
            if (!params[i].getType().isAssignableFrom(argType)) {
                return false;
            }
        }
        return true;

    }

    @Override
    public void dispose() {

    }

    public static class FactoryBeanMethodCycleException extends Exception {

    }

}
