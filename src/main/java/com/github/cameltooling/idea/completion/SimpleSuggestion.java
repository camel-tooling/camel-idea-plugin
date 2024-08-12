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

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * A class representing the suggestion of a given item that can be proposed by several supported
 * {@code CompletionProvider}. It aims to hold the name and the description of the item that has been suggested to be
 * able to easily generate its corresponding documentation.
 */
public class SimpleSuggestion {

    /**
     * The name of the suggested item.
     */
    private final String name;
    /**
     * The supplier of the description of the suggested item.
     */
    private final Supplier<String> descriptionSupplier;
    /**
     * The lookup string to use.
     */
    private final String lookupString;

    /**
     * Construct a {@code SimpleSuggestion} with the given parameters.
     * @param name the name of the suggested item
     * @param descriptionSupplier the supplier of the description of the suggested item
     * @param lookupString the lookup string to use
     */
    public SimpleSuggestion(@NotNull String name, @NotNull Supplier<String> descriptionSupplier,
                            @NotNull String lookupString) {
        this.name = name;
        this.descriptionSupplier = descriptionSupplier;
        this.lookupString = lookupString;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return descriptionSupplier.get();
    }

    @Override
    public String toString() {
        return lookupString;
    }
}
