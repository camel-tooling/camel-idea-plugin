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
package org.apache.camel.idea.service.extension.camel;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;

abstract class CamelIdeaUtils {

    protected static final Logger LOG = Logger.getInstance(CamelIdeaUtils.class);

    protected static final String[] ROUTE_START = new String[]{"from", "fromF"};
    protected static final String[] SIMPLE_PREDICATE = new String[]{"completion", "completionPredicate", "when", "onWhen", "handled", "continued", "retryWhile", "filter", "validate", "loopDoWhile"};
    protected static final String[] CONSUMER_ENDPOINT = new String[]{"from", "fromF", "interceptFrom", "pollEnrich"};
    protected static final String[] PRODUCER_ENDPOINT = new String[]{"to", "toF", "toD", "enrich", "interceptSendToEndpoint", "wireTap", "deadLetterChannel"};
    protected static final String[] STRING_FORMAT_ENDPOINT = new String[]{"fromF", "toF", "format"};

    protected static final String[] ACCEPTED_NAMESPACES = new String[]{
        "http://camel.apache.org/schema/spring",
        "http://camel.apache.org/schema/blueprint",
        "http://www.springframework.org/schema/beans",
        "http://www.osgi.org/xmlns/blueprint"
    };

    /**
     * Count the number of siblings there are in the chain the element has
     *
     * @param element the element
     * @return number of siblings added up in the chain
     */
    protected int countSiblings(PsiElement element) {
        int count = 0;
        PsiElement sibling = element.getNextSibling();
        while (sibling != null) {
            count++;
            sibling = sibling.getNextSibling();
        }
        return count;
    }

}
