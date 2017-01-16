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
package org.apache.camel.idea.annotator;

import org.apache.camel.catalog.EndpointValidationResult;

/**
 * Constructing an error messages to show in the {@link com.intellij.lang.annotation.Annotator}
 * message in the editor from the validation result.
 */
public interface CamelAnnotatorEndpointMessage<T> {

    /**
     * Return error messaged constructed to match the validation result.
     *
     * @param endpointValidationResult - The validation result return from the {@link org.apache.camel.catalog.CamelCatalog} validator
     * @param valueObj - The key and value object validated.
     */
    String getErrorMessage(EndpointValidationResult endpointValidationResult, T valueObj);

    /**
     * Is this an error
     */
    default boolean isErrorLevel() {
        return true;
    }

    /**
     * Is this a warning
     */
    default boolean isWarnLevel() {
        return false;
    }

    /**
     * Is this a information
     */
    default boolean isInfoLevel() {
        return false;
    }
}
