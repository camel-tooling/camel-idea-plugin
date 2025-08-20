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
package com.github.cameltooling.idea.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.camel.catalog.JSonSchemaResolver;
import org.apache.camel.catalog.impl.CatalogHelper;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.MainModel;
import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.util.StringUtils.fromKebabToCamelCase;

/**
 * {@code SpringBootJSonSchemaResolver} is a decorator of type {@link JSonSchemaResolver} allowing to retrieve the
 * files {@code META-INF/spring-configuration-metadata.json} and {@code META-INF/additional-spring-configuration-metadata.json}
 * corresponding to the <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">Spring Configuration Metadata</a>
 * from Camel libraries to generate the content of the main JSON schema and of the component JSON schemas that are
 * specific to Spring Boot
 */
class SpringBootJSonSchemaResolver implements JSonSchemaResolver {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(SpringBootJSonSchemaResolver.class);

    /**
     * The location of the main Spring configuration metadata.
     */
    private static final String SPRING_CONFIGURATION_METADATA = "META-INF/spring-configuration-metadata.json";
    /**
     * The location of the additional Spring configuration metadata.
     */
    private static final String ADDITIONAL_SPRING_CONFIGURATION_METADATA = "META-INF/additional-spring-configuration-metadata.json";

    /**
     * The supplier of the {@link ClassLoader} used to load the Spring configuration metadata.
     */
    private final Supplier<ClassLoader> classLoaderSupplier;
    /**
     * The {@link JSonSchemaResolver} to decorate.
     */
    private final JSonSchemaResolver delegate;

    /**
     * Construct a {@code SpringBootJSonSchemaResolver} with the given parameters.
     * @param classLoaderSupplier the supplier of the {@link ClassLoader} used to load the Spring configuration metadata.
     * @param delegate the {@link JSonSchemaResolver} to decorate.
     */
    SpringBootJSonSchemaResolver(Supplier<ClassLoader> classLoaderSupplier, JSonSchemaResolver delegate) {
        this.classLoaderSupplier = classLoaderSupplier;
        this.delegate = delegate;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        delegate.setClassLoader(classLoader);
    }

    @Override
    public String getComponentJSonSchema(String name) {
        return toComponentJSonSchema(
            name, getSpringConfigurationMetadata(String.format("camel-%s-starter", name)),
            delegate.getComponentJSonSchema(name)
        );
    }

    @Override
    public String getDataFormatJSonSchema(String name) {
        return delegate.getDataFormatJSonSchema(name);
    }

    @Override
    public String getLanguageJSonSchema(String name) {
        return delegate.getLanguageJSonSchema(name);
    }

    @Override
    public String getOtherJSonSchema(String name) {
        return delegate.getOtherJSonSchema(name);
    }

    @Override
    public String getModelJSonSchema(String name) {
        return delegate.getModelJSonSchema(name);
    }

    @Override
    public String getMainJsonSchema() {
        final String schema = toMainJsonSchema(getSpringConfigurationMetadata("camel-spring-boot", true));
        return schema == null ? delegate.getMainJsonSchema() : schema;
    }

    @Override
    public String getJBangJsonSchema() {
        final String schema = toMainJsonSchema(getSpringConfigurationMetadata("camel-spring-boot", true));
        return schema == null ? delegate.getJBangJsonSchema() : schema;
    }

    @Override
    public String getTransformerJSonSchema(String name) {
        return delegate.getTransformerJSonSchema(name);
    }

    @Override
    public String getDevConsoleJSonSchema(String name) {
        return delegate.getDevConsoleJSonSchema(name);
    }

    @Override
    public String getPojoBeanJSonSchema(String name) {
        return delegate.getPojoBeanJSonSchema(name);
    }

    /**
     * Extract the properties found in the Spring configuration metadata, remove the properties whose name doesn't
     * start with {@code camel.component.${component-name}.} or contains {@code customizer} and inject the resulting
     * properties into the original Json schema if they don't yet exist.
     *
     * @param componentName the name of the component for which the Json schema is expected.
     * @param springConfigurationMetadata the content of the Spring configuration metadata to merge with the original
     *                                    Json schema
     * @param originalComponentJSonSchema the content of the original component Json schema for the given component.
     * @return the merge result of the original component Json schema with the Spring configuration metadata,
     * {@code originalComponentJSonSchema} if one parameter is {@code null} or cannot be parsed.
     */
    private String toComponentJSonSchema(final String componentName, final String springConfigurationMetadata,
                                         final String originalComponentJSonSchema) {
        if (springConfigurationMetadata == null || originalComponentJSonSchema == null || componentName == null) {
            return originalComponentJSonSchema;
        }
        try {
            final JsonObject jsonObject = (JsonObject) Jsoner.deserialize(springConfigurationMetadata);
            if (jsonObject == null) {
                return originalComponentJSonSchema;
            }
            final ComponentModel model = JsonMapper.generateComponentModel(originalComponentJSonSchema);
            final List<ComponentModel.ComponentOptionModel> componentOptions = model.getComponentOptions();
            final Collection<?> properties = Optional.<Collection<?>>ofNullable(jsonObject.getCollection("properties"))
                .orElse(List.of());
            final String prefix = String.format("camel.component.%s.", componentName);
            final Set<String> targetNames = properties.stream()
                .map(JsonObject.class::cast)
                .map(property -> property.getString("name"))
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.substring(prefix.length()))
                .map(StringUtils::fromKebabToCamelCase)
                .collect(Collectors.toSet());

            final Predicate<ComponentModel.ComponentOptionModel> isKindProperty = option -> "property".equals(option.getKind());
            // Remove all properties that are not in the new source
            componentOptions.removeIf(option -> isKindProperty.test(option) && !targetNames.contains(option.getName()));
            final Set<String> existingNames = componentOptions.stream()
                .filter(isKindProperty)
                .map(BaseOptionModel::getName)
                .collect(Collectors.toSet());
            for (Object o : properties) {
                final JsonObject property = (JsonObject) o;
                String name = property.getString("name");
                if (!name.startsWith(prefix) || name.contains("customizer")) {
                    continue;
                }
                name = fromKebabToCamelCase(name.substring(prefix.length()));
                if (existingNames.contains(name)) {
                    // Already in so let's skip its creating
                    continue;
                }
                final ComponentModel.ComponentOptionModel option = new ComponentModel.ComponentOptionModel();
                option.setName(name);
                option.setKind("property");
                option.setJavaType(property.getString("type"));
                option.setDescription(property.getString("description"));
                option.setDefaultValue(property.get("defaultValue"));
                option.setEnums(getEnums(option.getJavaType()));
                option.setDeprecated(property.containsKey("deprecation"));
                componentOptions.add(option);
            }
            return JsonMapper.createParameterJsonSchema(model);
        } catch (DeserializationException e) {
            LOG.warn("Could not deserialize the Spring Configuration Metadata: " + e.getMessage());
        }
        return originalComponentJSonSchema;
    }

    /**
     * Merge into one main Json schema all groups and properties whose name matches with the expectations
     * (see {@link #isNameToIgnore(String)} for more details). All duplicates are skipped.
     *
     * @param allSpringConfigurationMetadata the content of all Spring configuration metadata that could be found in Camel
     *                                       libraries.
     * @return the content of a main Json schema corresponding to all groups and properties that could be found in the
     * given content of Spring configuration metadata, {@code null} if the given list is empty or the first Spring
     * configuration metadata corresponding to the main one cannot be parsed.
     */
    private String toMainJsonSchema(final List<String> allSpringConfigurationMetadata) {
        if (allSpringConfigurationMetadata.isEmpty()) {
            return null;
        }
        try {
            final Iterator<String> allSpringConfigurationMetadataIterator = allSpringConfigurationMetadata.iterator();
            final String mainSpringConfigurationMetadata = allSpringConfigurationMetadataIterator.next();
            final JsonObject jsonObject = (JsonObject) Jsoner.deserialize(mainSpringConfigurationMetadata);
            if (jsonObject == null) {
                return null;
            }
            final MainModel model = JsonMapper.generateMainModel(mainSpringConfigurationMetadata);
            final Set<String> existingGroups = new HashSet<>();
            for (Iterator<MainModel.MainGroupModel> iterator = model.getGroups().iterator(); iterator.hasNext();) {
                final MainModel.MainGroupModel group = iterator.next();
                final String name = group.getName();
                if (isNameToIgnore(name) || !existingGroups.add(name)) {
                    // Remove all useless groups
                    iterator.remove();
                }
            }
            final Collection<?> allProperties = Optional.<Collection<?>>ofNullable(jsonObject.getCollection("properties"))
                .orElse(List.of());
            final List<JsonObject> properties = allProperties.stream()
                .map(JsonObject.class::cast)
                .collect(Collectors.toList());
            final Set<String> existingProperties = new HashSet<>();
            for (Iterator<MainModel.MainOptionModel> iterator = model.getOptions().iterator(); iterator.hasNext();) {
                final MainModel.MainOptionModel option = iterator.next();
                final String name = option.getName();
                if (isNameToIgnore(name) || !existingProperties.add(name)) {
                    // Remove all useless properties
                    iterator.remove();
                    continue;
                }
                option.setJavaType(
                    properties.stream()
                        .filter(p -> name.equals(p.getString("name")))
                        .map(p -> p.getString("type"))
                        .findFirst()
                        .orElse(null)
                );
                option.setEnums(getEnums(option.getJavaType()));
            }
            // Add the rest of the Camel related main metadata
            while (allSpringConfigurationMetadataIterator.hasNext()) {
                addSpringConfigurationMetadata(
                    model, allSpringConfigurationMetadataIterator.next(), existingGroups, existingProperties
                );
            }
            return JsonMapper.createJsonSchema(model);
        } catch (DeserializationException e) {
            LOG.warn("Could not deserialize the Spring Configuration Metadata: " + e.getMessage());
        }
        return null;
    }

    /**
     * Add the groups and properties found in the given Spring configuration metadata if their name matches with the
     * expectations (see {@link #isNameToIgnore(String)} for more details) and have not been added already.
     *
     * @param model the model to which the groups and properties should be added.
     * @param springConfigurationMetadata the Spring configuration metadata from which the groups and properties are
     *                                    extracted.
     * @param existingGroups the name of groups that have already been added to the model.
     * @param existingProperties the name of properties that have already been added to the model.
     */
    private void addSpringConfigurationMetadata(final MainModel model,
                                                final String springConfigurationMetadata,
                                                final Set<String> existingGroups,
                                                final Set<String> existingProperties) {
        try {
            final JsonObject metadata = (JsonObject) Jsoner.deserialize(springConfigurationMetadata);
            if (metadata == null) {
                return;
            }
            for (Object o : Optional.<Collection<?>>ofNullable(metadata.getCollection("groups")).orElse(List.of())) {
                final JsonObject groupObject = (JsonObject) o;
                final String name = groupObject.getString("name");
                if (isNameToIgnore(name) || !existingGroups.add(name)) {
                    continue;
                }
                final MainModel.MainGroupModel group = new MainModel.MainGroupModel();
                group.setName(name);
                group.setDescription(groupObject.getString("description"));
                group.setSourceType(groupObject.getString("sourceType"));
                model.addGroup(group);
            }
            for (Object o : Optional.<Collection<?>>ofNullable(metadata.getCollection("properties")).orElse(List.of())) {
                final JsonObject propertyObject = (JsonObject) o;
                final String name = propertyObject.getString("name");
                if (isNameToIgnore(name) || !existingProperties.add(name)) {
                    continue;
                }
                final MainModel.MainOptionModel option = new MainModel.MainOptionModel();
                option.setName(name);
                option.setDescription(propertyObject.getString("description"));
                option.setType(propertyObject.getString("type"));
                option.setJavaType(propertyObject.getString("type"));
                option.setSourceType(propertyObject.getString("sourceType"));
                option.setDefaultValue(propertyObject.get("defaultValue"));
                option.setEnums(getEnums(option.getJavaType()));
                option.setDeprecated(propertyObject.containsKey("deprecation"));
                model.addOption(option);
            }
        } catch (DeserializationException e) {
            LOG.warn("Could not deserialize the Spring Configuration Metadata to add: " + e.getMessage());
        }
    }

    /**
     * Loads the class corresponding to the given java type from the {@code ClassLoader} provided by the supplier and
     * if it is an enum, then retrieve the list of constants, convert them into a {@code String} and return the
     * corresponding list.
     *
     * @param javaType the java type for which the enum constants are expected.
     * @return the list of enum constants corresponding to the given java type if it can be found and is an enum,
     * {@code null} otherwise.
     */
    private @Nullable List<String> getEnums(final String javaType) {
        if (javaType == null) {
            return null;
        }
        try {
            final Class<?> loadedClass = classLoaderSupplier.get().loadClass(javaType);
            if (loadedClass.isEnum()) {
                return Arrays.stream(loadedClass.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("The java type %s could not be found", javaType), e);
            }
        }
        return null;
    }

    /**
     * @param lib the name of the library for which the Spring configuration metadata is expected.
     * @return the content of {@code META-INF/spring-configuration-metadata.json} for the given library if it can be
     * found, {@code null} otherwise.
     */
    private String getSpringConfigurationMetadata(final String lib) {
        final List<String> result = getSpringConfigurationMetadata(lib, false);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * @param lib the library for which the Spring configuration metadata is expected.
     * @param includeOthers indicates whether other Spring configuration metadata should be returned.
     * @return a list of content of {@code META-INF/spring-configuration-metadata.json} and
     * {@code META-INF/additional-spring-configuration-metadata.json} that can be found in Camel libraries if
     * {@code includeOthers} is {@code true} only the content of {@code META-INF/spring-configuration-metadata.json} in
     * the requested library if {@code includeOthers} is {@code false}. In all cases, the content of
     * {@code META-INF/spring-configuration-metadata.json} in the requested library is always the first element in the
     * list if it could be found otherwise an empty list is returned.
     */
    private List<String> getSpringConfigurationMetadata(final String lib, final boolean includeOthers) {
        final ClassLoader projectClassloader = classLoaderSupplier.get();
        if (projectClassloader == null) {
            return List.of();
        }
        try {
            return getSpringConfigurationMetadata(projectClassloader, lib, includeOthers);
        } catch (IOException e) {
            LOG.warn("Could not load the Spring Configuration Metadata: " + e.getMessage());
        }
        return List.of();
    }

    /**
     * @param name the name to test.
     * @return {@code true} if the given name doesn't contain {@code customizer} or doesn't start like a property
     * for language, data format or component.
     */
    private static boolean isNameToIgnore(final String name) {
        return name.contains("customizer") || name.startsWith("camel.component")
            || name.startsWith("camel.language") || name.startsWith("camel.dataformat");
    }

    /**
     * @param projectClassloader the {@link ClassLoader} from which the content of
     * {@code META-INF/spring-configuration-metadata.json} and {@code META-INF/additional-spring-configuration-metadata.json}
     *                           are retrieved.
     * @param lib the library for which the Spring configuration metadata is expected.
     * @param includeOthers indicates whether other Spring configuration metadata should be returned.
     * @return a list of content of {@code META-INF/spring-configuration-metadata.json} and
     * {@code META-INF/additional-spring-configuration-metadata.json} that can be found in Camel libraries if
     * {@code includeOthers} is {@code true} only the content of {@code META-INF/spring-configuration-metadata.json} in
     * the requested library if {@code includeOthers} is {@code false}. In all cases, the content of
     * {@code META-INF/spring-configuration-metadata.json} in the requested library is always the first element in the
     * list if it could be found otherwise an empty list is returned.
     * @throws IOException if the resources could not be retrieved from the {@link ClassLoader} or their content could
     * not be loaded.
     */
    @NotNull
    private static List<String> getSpringConfigurationMetadata(final ClassLoader projectClassloader,
                                                               final String lib,
                                                               final boolean includeOthers) throws IOException {
        final List<String> result = new ArrayList<>();
        Enumeration<URL> resources = projectClassloader.getResources(SPRING_CONFIGURATION_METADATA);
        final String content = String.format("/%s/", lib);
        boolean found = false;
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            final String path = resource.getPath();
            if (isCamelLib(path)) {
                if (path.contains(content)) {
                    result.add(0, CatalogHelper.loadText(resource.openStream()));
                    found = true;
                } else if (includeOthers) {
                    result.add(CatalogHelper.loadText(resource.openStream()));
                }
            }
        }
        if (!found) {
            LOG.debug(String.format("The Spring Configuration Metadata of the lib '%s' could not be found.", lib));
            return List.of();
        } else if (includeOthers) {
            // Add the additional spring configuration
            resources = projectClassloader.getResources(ADDITIONAL_SPRING_CONFIGURATION_METADATA);
            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                if (isCamelLib(resource.getPath())) {
                    result.add(CatalogHelper.loadText(resource.openStream()));
                }
            }
        }
        return result;
    }

    /**
     * @param path the path of the lib to test
     * @return {@code true} if the path matches with the path of a Camel library, {@code false} otherwise.
     */
    private static boolean isCamelLib(final String path) {
        return path.contains("/camel-");
    }
}
