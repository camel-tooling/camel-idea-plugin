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
package com.github.cameltooling.idea.reference.blueprint.model;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an id of a bean, which can be referenced from other places. Mainly <bean id="[THIS]">
 *
 *     TODO: rename to something more suitable, like BeanDefinition
 */
public class ReferenceableBeanId {

    private final XmlTag beanTag;
    private final XmlAttributeValue beanIdAttribute;
    private final String id;

    public ReferenceableBeanId(@NotNull XmlTag beanTag, @NotNull XmlAttributeValue beanIdAttribute) {
        this.beanTag = beanTag;
        this.beanIdAttribute = beanIdAttribute;
        this.id = beanIdAttribute.getValue();
    }

    public XmlTag getBeanTag() {
        return beanTag;
    }

    @NotNull
    public PsiElement getElement() {
        return beanIdAttribute;
    }

    @NotNull
    public String getId() {
        return id;
    }

}
