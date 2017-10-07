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
package org.apache.camel.idea.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.apache.camel.idea.util.StringUtils;

public final class QueryUtils {

    private QueryUtils() {
    }

    /**
     * Validate if the query contain a known camel component
     */
    public static boolean isQueryContainingCamelComponent(Project project, String query) {
        // is this a possible Camel endpoint uri which we know
        if (query != null && !query.isEmpty()) {
            String componentName = StringUtils.asComponentName(query);
            if (componentName != null && ServiceManager.getService(project, CamelCatalogService.class).get().findComponentNames().contains(componentName)) {
                return true;
            }
        }
        return false;
    }
}
