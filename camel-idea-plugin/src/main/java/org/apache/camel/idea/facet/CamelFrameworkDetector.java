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
package org.apache.camel.idea.facet;

import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileContent;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;

public class CamelFrameworkDetector extends FacetBasedFrameworkDetector<CamelFacet, CamelFacetConfiguration> {

    protected CamelFrameworkDetector() {
        super("camel");
    }

    @NotNull
    @Override
    public FacetType<CamelFacet, CamelFacetConfiguration> getFacetType() {
        return CamelFacetType.getInstance();
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return StdFileTypes.XML;
    }

    @NotNull
    @Override
    public ElementPattern<FileContent> createSuitableFilePattern() {
        return FileContentPattern.fileContent().with(new PatternCondition<FileContent>("") {
            @Override
            public boolean accepts(@NotNull final FileContent fileContent, final ProcessingContext context) {
                return CamelIdeaUtils.isCamelRouteStart(fileContent.getPsiFile());
            }
        });

    }
}
