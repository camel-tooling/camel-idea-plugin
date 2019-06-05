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
package com.github.cameltooling.idea.reference;

import java.util.Objects;
import javax.swing.Icon;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.RenameableFakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A fake psi element which wraps a real psi element (a camel direct endpoint declaration) and is used as a target
 * when resolving references to direct endpoints.
 *
 * Main purpose is to provide the ability to show a custom type name by overriding {@link FakeCamelPsiElement#getTypeName()}
 * method - this is shown in multiple places - for example when using the Refactor -> Rename functionality.
 */
public abstract class FakeCamelPsiElement extends RenameableFakePsiElement {

    protected final PsiElement element;

    public FakeCamelPsiElement(@NotNull PsiElement element) {
        super(null);
        this.element = element;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return element.getNavigationElement();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public boolean isValid() {
        return element.isValid();
    }

    @Override
    public PsiElement getContext() {
        return element.getNavigationElement();
    }

    @Override
    public PsiFile getContainingFile() {
        return element.getNavigationElement().getContainingFile();
    }

    @NotNull
    @Override
    public Project getProject() {
        return element.getProject();
    }

    @Nullable
    @Override
    public String getLocationString() {
        PsiFile file = getContainingFile();
        if (file != null) {
            return file.getName();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FakeCamelPsiElement that = (FakeCamelPsiElement) o;
        return Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element);
    }

}
