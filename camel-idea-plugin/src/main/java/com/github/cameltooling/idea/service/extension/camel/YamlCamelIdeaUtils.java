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
package com.github.cameltooling.idea.service.extension.camel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.YamlPatternConditions;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

public class YamlCamelIdeaUtils extends CamelIdeaUtils implements CamelIdeaUtilsExtension {

    private static final List<String> YAML_ROUTES = Arrays.asList(
        "routes",
        "routeConfigurations",
        "route",
        "routeConfiguration");
    /**
     * All keys representing the potential producers.
     */
    private static final String[] PRODUCERS = {
        "to", "tod", "toD", "to-d", "intercept-send-to-endpoint", "interceptSendToEndpoint", "wire-tap", "wireTap",
        "dead-letter-channel", "deadLetterChannel"
    };
    /**
     * All keys representing the uri of the producers.
     */
    private static final String[] PRODUCERS_URI = {"uri", "dead-letter-uri"};
    /**
     * All keys representing the potential consumers.
     */
    private static final String[] CONSUMERS = {"from", "intercept-from", "interceptFrom"};
    /**
     * All keys whose value can be a URI.
     */
    private static final String[] PLACE_FOR_ENDPOINT_URI = {"uri", "dead-letter-uri", "from", "to"};

    /**
     * The pattern to identify a camel route start corresponding to a YAMLKeyValue element
     * whose first child is the key "from" or "rest".
     */
    private static final ElementPattern<YAMLKeyValue> CAMEL_ROUTE_START_PATTERN =
        PlatformPatterns.psiElement(YAMLKeyValue.class)
            .with(
                YamlPatternConditions.withFirstChild(
                    StandardPatterns.or(
                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .withText("from"),
                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .withText("rest")
                    )
                )
            );
    /**
     * The pattern to identify a consumer endpoint corresponding to a YAMLKeyValue element
     * whose children are "from" as key and a URI as value or whose parent YAMLKeyValue element
     * has for first child a key among the possible consumers' key and whose first child is the
     * key "uri".
     */
    private static final ElementPattern<YAMLKeyValue> CONSUMER_ENDPOINT =
        StandardPatterns.or(
            PlatformPatterns.psiElement(YAMLKeyValue.class)
                .withSuperParent(
                    2,
                    PlatformPatterns.psiElement(YAMLKeyValue.class)
                        .with(
                            YamlPatternConditions.withFirstChild(
                                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                    .with(YamlPatternConditions.withText(CONSUMERS))
                            )
                        )
                )
                .with(
                    YamlPatternConditions.withFirstChild(
                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .withText("uri")
                    )
                ),
            PlatformPatterns.psiElement(YAMLKeyValue.class)
                .with(
                    YamlPatternConditions.withFirstChild(
                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .withText("from")
                    )
                )
                .with(
                    YamlPatternConditions.withLastChild(
                        PlatformPatterns.psiElement()
                            .with(
                                YamlPatternConditions.withElementType(
                                    YAMLElementTypes.SCALAR_PLAIN_VALUE, YAMLElementTypes.SCALAR_QUOTED_STRING
                                )
                            )
                    )
                )
        );
    /**
     * The pattern to identify a producer endpoint corresponding to a YAMLKeyValue element
     * whose children are "from" as key and a URI as value or whose parent YAMLKeyValue element
     * has for first child a key among the producer consumers' key and whose first child is a
     * key among the producer consumers' uri key.
     */
    private static final ElementPattern<YAMLKeyValue> PRODUCER_ENDPOINT =
        StandardPatterns.or(
            PlatformPatterns.psiElement(YAMLKeyValue.class)
                .withSuperParent(
                    2,
                    PlatformPatterns.psiElement(YAMLKeyValue.class)
                        .with(
                            YamlPatternConditions.withFirstChild(
                                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                                    .with(YamlPatternConditions.withText(PRODUCERS))
                            )
                        )
                )
                .with(
                    YamlPatternConditions.withFirstChild(
                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .with(YamlPatternConditions.withText(PRODUCERS_URI))
                    )
                ),
            PlatformPatterns.psiElement(YAMLKeyValue.class)
                .with(
                    YamlPatternConditions.withFirstChild(
                        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
                            .withText("to")
                    )
                )
                .with(
                    YamlPatternConditions.withLastChild(
                        PlatformPatterns.psiElement()
                            .with(
                                YamlPatternConditions.withElementType(
                                    YAMLElementTypes.SCALAR_PLAIN_VALUE, YAMLElementTypes.SCALAR_QUOTED_STRING
                                )
                            )
                    )
                )
        );

    @Override
    public boolean isCamelFile(PsiFile file) {
        if (file != null && YAMLFileType.YML.equals(file.getFileType())) {
            YAMLFile yamlFile = (YAMLFile) file;
            List<YAMLDocument> yamlDocuments = yamlFile.getDocuments();
            return yamlDocuments.stream().anyMatch(document -> {
                YAMLValue value = document.getTopLevelValue();
                if (!(value instanceof YAMLSequence)) {
                    return false;
                }
                YAMLSequence sequence = (YAMLSequence) value;
                List<YAMLSequenceItem> sequenceItems = sequence.getItems();
                if (sequenceItems.isEmpty()) {
                    return false;
                }
                YAMLSequenceItem firstItem = sequenceItems.get(0);
                Collection<YAMLKeyValue> keysValues = firstItem.getKeysValues();
                if (keysValues.isEmpty()) {
                    return false;
                }
                YAMLKeyValue firstKeyValue = keysValues.iterator().next();
                if (firstKeyValue == null) {
                    return false;
                }
                return YAML_ROUTES.contains(firstKeyValue.getKeyText());
            });
        }

        return false;
    }

    @Override
    public boolean isCamelRouteStart(PsiElement element) {
        return CAMEL_ROUTE_START_PATTERN.accepts(element);
    }

    @Override
    public boolean isCamelRouteStartExpression(PsiElement element) {
        if (element instanceof YAMLKeyValue) {
            return isCamelRouteStart(element);
        } else if (element.getParent() instanceof YAMLKeyValue) {
            return isCamelRouteStart(element.getParent());
        }
        return false;
    }

    @Override
    public boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart) {
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        if (keyValue == null || excludeRouteStart && isCamelRouteStart(keyValue)) {
            return false;
        }
        return getIdeaUtils().findFirstParent(keyValue, false, this::isCamelRouteStart, PsiFile.class::isInstance) != null;
    }

    @Override
    public boolean isCamelExpression(PsiElement element, String language) {
        YAMLKeyValue keyValue;
        if (element instanceof YAMLKeyValue) {
            keyValue = (YAMLKeyValue) element;
        } else {
            keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        }
        if (keyValue != null) {
            String name = keyValue.getKeyText();
            // extra check for simple language
            if ("simple".equals(language) && "log".equals(name)) {
                return true;
            }
            return language.equals(name);
        }
        return false;
    }

    @Override
    public boolean isCamelExpressionUsedAsPredicate(PsiElement element, String language) {
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        if (keyValue != null) {
            // if it's coming from the log EIP then it's not a predicate
            if ("simple".equals(language) && keyValue.getKeyText().equals("log")) {
                return false;
            }

            // special for loop which can be both expression or predicate
            if (getIdeaUtils().hasParentYAMLKeyValue(keyValue, "loop")) {
                YAMLKeyValue parentYAMLKeyValue = PsiTreeUtil.getParentOfType(keyValue, YAMLKeyValue.class);
                if (parentYAMLKeyValue != null) {
                    YAMLMapping parent = PsiTreeUtil.findChildOfType(parentYAMLKeyValue, YAMLMapping.class);
                    if (parent != null) {
                        YAMLKeyValue doWhile = parent.getKeyValueByKey("do-while");
                        return doWhile != null && "true".equalsIgnoreCase(doWhile.getValueText());
                    }
                }
            }
            return Arrays.stream(PREDICATE_EIPS).anyMatch(n -> getIdeaUtils().hasParentYAMLKeyValue(keyValue, n));
        }
        return false;
    }

    @Override
    public boolean isConsumerEndpoint(PsiElement element) {
        final YAMLKeyValue keyValue;
        if (element instanceof YAMLKeyValue) {
            keyValue = (YAMLKeyValue) element;
        } else {
            keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        }
        if (keyValue != null) {
            return getIdeaUtils().hasParentYAMLKeyValue(keyValue, "poll-enrich", "pollEnrich")
                || getIdeaUtils().isURIYAMLKeyValue(keyValue, CONSUMERS);
        }

        return false;
    }

    @Override
    public boolean isProducerEndpoint(PsiElement element) {
        final YAMLKeyValue keyValue;
        if (element instanceof YAMLKeyValue) {
            keyValue = (YAMLKeyValue) element;
        } else {
            keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
        }
        if (keyValue != null) {
            return getIdeaUtils().hasParentYAMLKeyValue(keyValue, "enrich")
                || getIdeaUtils().isURIYAMLKeyValue(keyValue, PRODUCERS);
        }
        return false;
    }

    /**
     * @param element the element to test.
     * @return {@code true} if the given element is a scalar key, {@code false} otherwise.
     */
    @Override
    public boolean isCamelLineMarker(PsiElement element) {
        final ASTNode node = element.getNode();
        return node != null && node.getElementType() == YAMLTokenTypes.SCALAR_KEY;
    }

    @Override
    public boolean skipEndpointValidation(PsiElement element) {
        return false;
    }

    @Override
    public boolean isFromStringFormatEndpoint(PsiElement element) {
        return false;
    }

    @Override
    public boolean acceptForAnnotatorOrInspection(PsiElement element) {
        return false;
    }

    @Override
    public boolean isExtensionEnabled() {
        final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.plugins.yaml"));
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public PsiClass getBeanClass(PsiElement element) {
        return null;
    }

    @Override
    public PsiElement getPsiElementForCamelBeanMethod(PsiElement element) {
        return null;
    }

    @Override
    public List<PsiElement> findEndpointUsages(Module module, Predicate<String> uriCondition) {
        return findEndpoints(module, PRODUCER_ENDPOINT, uriCondition);
    }

    @Override
    public List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition) {
        return findEndpoints(module, CONSUMER_ENDPOINT, uriCondition);
    }

    private List<PsiElement> findEndpoints(Module module, ElementPattern<YAMLKeyValue> pattern, Predicate<String> uriCondition) {
        final List<PsiElement> result = new ArrayList<>();
        IdeaUtils.getService().iterateYamlFiles(
            module,
            file -> PsiTreeUtil.processElements(
                file,
                element -> {
                    if (pattern.accepts(element) && uriCondition.test(((YAMLKeyValue) element).getValueText())) {
                        result.add(element.getLastChild());
                    }
                    return true;
                }
            )
        );
        return result;
    }

    @Override
    public boolean isPlaceForEndpointUri(PsiElement location) {
        YAMLFile file = PsiTreeUtil.getParentOfType(location, YAMLFile.class);
        if (file == null) {
            return false;
        }
        YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(location, YAMLKeyValue.class);
        if (keyValue == null) {
            return false;
        }
        return Arrays.asList(PLACE_FOR_ENDPOINT_URI).contains(keyValue.getKeyText())
            && isInsideCamelRoute(location, false);
    }

    private IdeaUtils getIdeaUtils() {
        return ApplicationManager.getApplication().getService(IdeaUtils.class);
    }

}
