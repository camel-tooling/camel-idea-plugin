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
package com.github.cameltooling.idea.gutter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.Icon;
import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.reference.blueprint.model.ReferenceableBeanId;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.util.BeanUtils;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Provider that adds the Camel icon in the gutter when it detects a BeanInject annotation and can find matching
 * bean declaration.
 */
public class BeanInjectLineMarkerProvider extends RelatedItemLineMarkerProvider {

    static final String MARKER_TOOLTIP_TEXT = "Navigate to bean declaration";

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {
        PsiAnnotation beanInjectAnnotation = getBeanInjectAnnotation(element);
        if (beanInjectAnnotation != null) {
            Module module = ModuleUtilCore.findModuleForPsiElement(element);
            if (module == null) {
                return;
            }
            BeanReference reference = getBeanReference(beanInjectAnnotation);
            Icon icon = CamelPreferenceService.getService().getCamelIcon();
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(icon)
                    .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                    .setCellRenderer(new GutterPsiElementListCellRenderer())
                    .setTooltipText(MARKER_TOOLTIP_TEXT)
                    .setPopupTitle("Choose Bean")
                    .setEmptyPopupText("Could not find the bean declaration");
            if (reference != null) {
                builder.setTargets(new NotNullLazyValue<Collection<? extends PsiElement>>() {
                    @NotNull
                    @Override
                    protected Collection<? extends PsiElement> compute() {
                        return wrapInCollection(reference.resolve());
                    }
                });
            } else {
                builder.setTargets(new NotNullLazyValue<Collection<? extends PsiElement>>() {
                    @NotNull
                    @Override
                    protected Collection<? extends PsiElement> compute() {
                        PsiType beanType = IdeaUtils.getService().findAnnotatedElementType(beanInjectAnnotation);
                        if (beanType != null) {
                            List<ReferenceableBeanId> beans = BeanUtils.getService().findReferenceableBeanIdsByType(module, beanType);
                            return beans.stream()
                                    .map(ReferenceableBeanId::getElement)
                                    .collect(Collectors.toList());
                        }
                        return Collections.emptyList();
                    }
                });

            }
            result.add(builder.createLineMarkerInfo(element));
        }
    }

    @NotNull
    private Collection<? extends PsiElement> wrapInCollection(PsiElement element) {
        if (element == null) {
            return Collections.emptyList();
        } else {
            return Collections.singleton(element);
        }
    }

    private BeanReference getBeanReference(PsiAnnotation beanInjectAnnotation) {
        return beanInjectAnnotation.getAttributes().stream()
                .filter(a -> a.getAttributeName().equals("value"))
                .filter(a -> a.getAttributeValue() != null && a.getAttributeValue() instanceof PsiAnnotationMemberValue)
                .map(a -> a.getAttributeValue())
                .map(e -> Arrays.stream(((PsiReferenceExpression)e).getReferences()))
                .flatMap(Function.identity())
                .filter(r -> r instanceof BeanReference)
                .map(r -> (BeanReference) r)
                .findAny().orElse(null);
    }

    private PsiAnnotation getBeanInjectAnnotation(PsiElement element) {
        if (element instanceof PsiIdentifier && element.getText().equals("BeanInject")) {
            PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
            if (annotation != null && CamelIdeaUtils.BEAN_INJECT_ANNOTATION.equals(annotation.getQualifiedName())) {
                return annotation;
            }
        }
        return null;
    }
}
