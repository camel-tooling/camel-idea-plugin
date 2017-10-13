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
package org.apache.camel.idea.inspection;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;
import static org.apache.camel.idea.util.IdeaUtils.isFromFileType;

/**
 * Camel inspection to validate Camel endpoints and languages such as simple.
 */
public class CamelInspection extends AbstractCamelInspection {

    public CamelInspection() {
    }

    public CamelInspection(boolean forceEnabled) {
        super(forceEnabled);
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Camel Inspection";
    }

    @Override
    boolean accept(PsiElement element) {
        // skip tokens as we want to only trigger on attributes and xml value if in XML mode
        boolean token = element instanceof XmlToken;
        if (token) {
            return false;
        }

        // we support java, xml, groovy, scala and kotlin
        return isFromFileType(element, CamelIdeaUtils.CAMEL_FILE_EXTENSIONS);
    }

}
