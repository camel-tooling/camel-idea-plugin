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
package com.github.cameltooling.idea.util;

import java.util.Arrays;

import com.intellij.lang.ASTNode;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * A utility call to define special pattern conditions used to build element pattern for Yaml content.
 */
public final class YamlPatternConditions {

    private YamlPatternConditions() {
    }

    /**
     * @param texts all the possible text contents that are accepted.
     * @return a {@code PatternCondition} that accepts elements that have for text content one of the provided
     * text contents.
     */
    public static <T extends PsiElement> PatternCondition<T> withText(String... texts) {
        return new TextPatternCondition<>(texts);
    }

    /**
     * @param types all the possible element types that are accepted.
     * @return a {@code PatternCondition} that accepts elements that have for element type one of the provided
     * element types.
     */
    public static <T extends PsiElement> PatternCondition<T> withElementType(IElementType... types) {
        return new ElementTypePatternCondition<>(types);
    }

    /**
     * @param patterns all the possible element patterns that are accepted.
     * @return a {@code PatternCondition} that accepts elements that are accepted by at least one of the provided
     * element patterns.
     */
    public static <T extends PsiElement> PatternCondition<T> or(ElementPattern<T>... patterns) {
        return new OrPatternCondition<>(patterns);
    }

    /**
     * @param pattern the pattern to validate against the first child.
     * @return a {@code PatternCondition} that accepts elements whose first child matches with the given pattern.
     */
    public static <T extends PsiElement> PatternCondition<T> withFirstChild(@NotNull ElementPattern<T> pattern) {
        return new FirstChildPatternCondition<>(pattern);
    }

    /**
     * @param key the key to validate the key value pair.
     * @param value the value to validate the key value pair.
     * @return a {@code PatternCondition} that accepts {@code YAMLKeyValue}s which match with the given key value pair.
     */
    public static <T extends YAMLKeyValue> PatternCondition<T> withPair(@NotNull String key, @NotNull String value) {
        return new PairPatternCondition<>(key, value);
    }

    /**
     * @param pattern the pattern to validate against the last child.
     * @return a {@code PatternCondition} that accepts elements whose last child matches with the given pattern.
     */
    public static <T extends PsiElement> PatternCondition<T> withLastChild(@NotNull ElementPattern<T> pattern) {
        return new LastChildPatternCondition<>(pattern);
    }

    /**
     * @param pattern the pattern to validate against the previous sibling.
     * @return a {@code PatternCondition} that accepts elements whose previous sibling matches with the given pattern.
     */
    public static <T extends PsiElement> PatternCondition<T> withPrevSibling(@NotNull ElementPattern<T> pattern) {
        return new PrevSiblingPatternCondition<>(pattern);
    }

    /**
     * {@code FirstChildPatternCondition} is a {@link PatternCondition} allowing to identify elements whose
     * first child matches with a specific pattern.
     */
    private static class FirstChildPatternCondition<T extends PsiElement> extends PatternCondition<T> {

        /**
         * The pattern to validate against the first child.
         */
        private final ElementPattern<T> pattern;

        /**
         * Construct a {@code FirstChildPatternCondition} with the given pattern.
         * @param pattern the pattern to validate against the first child.
         */
        FirstChildPatternCondition(@NotNull ElementPattern<T> pattern) {
            super("withFirstChild");
            this.pattern = pattern;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            final PsiElement firstChild = t.getFirstChild();
            return firstChild != null && pattern.accepts(firstChild);
        }
    }

    /**
     * {@code PairPatternCondition} is a {@link PatternCondition} allowing to identify {@code YAMLKeyValue}s which
     * match with the specific key value pair.
     */
    private static class PairPatternCondition<T extends YAMLKeyValue> extends PatternCondition<T> {

        /**
         * The key to validate the key value pair.
         */
        private final String key;
        /**
         * The value to validate the key value pair.
         */
        private final String value;

        /**
         * Construct a {@code PairPatternCondition} with the given key value pair.
         * @param key the key to validate the key value pair.
         * @param value the value to validate the key value pair.
         */
        PairPatternCondition(@NotNull String key, @NotNull String value) {
            super("withPair");
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            return key.equals(t.getKeyText()) && value.equals(t.getValueText());
        }
    }

    /**
     * {@code LastChildPatternCondition} is a {@link PatternCondition} allowing to identify elements whose
     * last child matches with a specific pattern.
     */
    private static class LastChildPatternCondition<T extends PsiElement> extends PatternCondition<T> {

        /**
         * The pattern to validate against the last child.
         */
        private final ElementPattern<T> pattern;

        /**
         * Construct a {@code LastChildPatternCondition} with the given pattern.
         * @param pattern the pattern to validate against the last child.
         */
        LastChildPatternCondition(@NotNull ElementPattern<T> pattern) {
            super("withLastChild");
            this.pattern = pattern;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            final PsiElement lastChild = t.getLastChild();
            return lastChild != null && pattern.accepts(lastChild);
        }
    }

    /**
     * {@code PrevSiblingPatternCondition} is a {@link PatternCondition} allowing to identify elements whose
     * previous sibling matches with a specific pattern.
     */
    private static class PrevSiblingPatternCondition<T extends PsiElement> extends PatternCondition<T> {

        /**
         * The pattern to validate against the previous sibling.
         */
        private final ElementPattern<T> pattern;

        /**
         * Construct a {@code PrevSiblingPatternCondition} with the given pattern.
         * @param pattern the pattern to validate against the previous sibling.
         */
        PrevSiblingPatternCondition(@NotNull ElementPattern<T> pattern) {
            super("withPrevSibling");
            this.pattern = pattern;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            final PsiElement prevSibling = t.getPrevSibling();
            return prevSibling != null && pattern.accepts(prevSibling);
        }
    }

    /**
     * {@code OrPatternCondition} is a {@link PatternCondition} allowing to identify elements which match
     * with at least one of the given patterns.
     */
    private static class OrPatternCondition<T extends PsiElement> extends PatternCondition<T> {

        /**
         * All the possible element patterns that are accepted.
         */
        private final ElementPattern[] patterns;

        /**
         * Construct a {@code OrPatternCondition} with the given patterns.
         * @param patterns all the possible element patterns that are accepted.
         */
        OrPatternCondition(@NotNull ElementPattern<T>... patterns) {
            super("withOr");
            this.patterns = patterns;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            return Arrays.stream(patterns).anyMatch(pattern -> pattern.accepts(t));
        }
    }

    /**
     * {@code ElementTypePatternCondition} is a {@link PatternCondition} allowing to identify elements whose
     * element type matches with one of the given element types.
     */
    private static class ElementTypePatternCondition<T extends PsiElement> extends PatternCondition<T> {

        /**
         * All the possible element types that are accepted.
         */
        private final IElementType[] types;

        /**
         * Construct a {@code ElementTypePatternCondition} with the given types.
         * @param types all the possible element types that are accepted.
         */
        ElementTypePatternCondition(IElementType... types) {
            super("withElementType");
            this.types = types;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            ASTNode node = t.getNode();
            return node != null && Arrays.stream(types).anyMatch(c -> c.equals(node.getElementType()));
        }
    }

    /**
     * {@code TextPatternCondition} is a {@link PatternCondition} allowing to identify elements whose
     * text content matches with one of the given text contents.
     */
    private static class TextPatternCondition<T extends PsiElement> extends PatternCondition<T> {

        /**
         * All the possible text contents that are accepted.
         */
        private final String[] texts;

        /**
         * Construct a {@code TextPatternCondition} with the given text contents.
         * @param texts all the possible text contents that are accepted.
         */
        TextPatternCondition(String... texts) {
            super("withText");
            this.texts = texts;
        }

        @Override
        public boolean accepts(@NotNull T t, ProcessingContext context) {
            return Arrays.stream(texts).anyMatch(c -> c.equals(t.getText()));
        }
    }
}
