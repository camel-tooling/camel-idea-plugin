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
package com.github.cameltooling.idea.completion;

import org.apache.camel.tooling.model.BaseOptionModel;

/**
 * A class representing the suggestion of a given option that can be proposed by several supported
 * {@code CompletionProvider}. It aims to hold the option that has been suggested to be able to easily generate its
 * corresponding documentation.
 */
public class OptionSuggestion {

    /**
     * The suggested option.
     */
    private final BaseOptionModel option;
    /**
     * The lookup string to use.
     */
    private final String lookupString;

    /**
     * Construct a {@code OptionSuggestion} with the given parameters.
     * @param option the suggested option
     * @param lookupString the lookup string to use
     */
    public OptionSuggestion(BaseOptionModel option, String lookupString) {
        this.option = option;
        this.lookupString = lookupString;
    }

    public BaseOptionModel getOption() {
        return option;
    }

    @Override
    public String toString() {
        return lookupString;
    }
}
