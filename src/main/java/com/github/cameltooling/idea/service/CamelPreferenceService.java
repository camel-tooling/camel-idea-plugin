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
package com.github.cameltooling.idea.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.*;

import com.github.cameltooling.idea.preference.propertyplaceholder.PropertyPlaceholderSettingsEntry;
import com.github.cameltooling.idea.reference.propertyplaceholder.XmlPropertyPlaceholderDefinition;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.Constants.OSGI_BLUEPRINT_NAMESPACE;

/**
 * Service for holding preference for this plugin.
 */
@State(
    name = "CamelPreferences",
    storages = {@Storage("apachecamelplugin.xml")})
public class CamelPreferenceService implements PersistentStateComponent<CamelPreferenceService>, Disposable {

    // Use a specific icon name to prevent naming collision with the Gradle plugin
    @Transient
    public static final Icon CAMEL_ICON = IconLoader.getIcon("/META-INF/camelPluginIcon.svg", CamelPreferenceService.class);
    @Transient
    private static final String[] DEFAULT_IGNORE_PROPERTIES = {
        // ignore java and logger prefixes
        "java.", "Logger.", "logger", "appender.", "rootLogger.",
        // ignore camel component/dataformat/language
        "camel.component.", "camel.dataformat.", "camel.language.",
        // ignore camel-main configuration prefixes
        "camel.main.", "camel.faulttolerance.", "camel.hystrix.", "camel.resilience4j.", "camel.rest.", "camel.health.", "camel.lra.", "camel.threadpool.",
        // ignore camel-spring-boot auto configuration prefixes
        "camel.springboot."
    };

    @Transient
    private static final String[] DEFAULT_EXCLUDE_FILE_PATTERN = {
        "**/log4j.properties", "**/log4j2.properties", "**/logging.properties"};
    private boolean realTimeEndpointValidation = true;
    private boolean realTimeSimpleValidation = true;
    private boolean realTimeJSonPathValidation = true;
    private boolean realTimeIdReferenceTypeValidation = true;
    private boolean realTimeBeanMethodValidationCheckBox = true;
    private boolean highlightCustomOptions = true;
    private boolean downloadCatalog = true;
    private boolean scanThirdPartyComponents = true;
    private boolean showCamelIconInGutter = true;
    private List<String> ignorePropertyList = new ArrayList<>();
    private List<String> excludePropertyFiles = new ArrayList<>();

    private static final List<PropertyPlaceholderSettingsEntry> DEFAULT_PROPERTY_PLACEHOLDERS = List.of(
            new PropertyPlaceholderSettingsEntry("${", "}", List.of(OSGI_BLUEPRINT_NAMESPACE), true)
    );

    // Settings for XML property placeholders persisted in plugin state
    private List<PropertyPlaceholderSettingsEntry> xmlPropertyPlaceholders = DEFAULT_PROPERTY_PLACEHOLDERS;

    protected CamelPreferenceService() { }

    public List<PropertyPlaceholderSettingsEntry> getXmlPropertyPlaceholders() {
        return xmlPropertyPlaceholders;
    }

    public void setXmlPropertyPlaceholders(List<PropertyPlaceholderSettingsEntry> xmlPropertyPlaceholders) {
        this.xmlPropertyPlaceholders = xmlPropertyPlaceholders;
    }

    public void setDefaultXmlPropertyPlaceholders() {
        this.xmlPropertyPlaceholders = DEFAULT_PROPERTY_PLACEHOLDERS;
    }

    public static CamelPreferenceService getService() {
        return ApplicationManager.getApplication().getService(CamelPreferenceService.class);
    }

    public boolean isRealTimeEndpointValidation() {
        return realTimeEndpointValidation;
    }

    public void setRealTimeEndpointValidation(boolean realTimeEndpointValidation) {
        this.realTimeEndpointValidation = realTimeEndpointValidation;
    }

    public boolean isRealTimeSimpleValidation() {
        return realTimeSimpleValidation;
    }

    public void setRealTimeSimpleValidation(boolean realTimeSimpleValidation) {
        this.realTimeSimpleValidation = realTimeSimpleValidation;
    }

    public boolean isRealTimeJSonPathValidation() {
        return realTimeJSonPathValidation;
    }

    public void setRealTimeJSonPathValidation(boolean realTimeJSonPathValidation) {
        this.realTimeJSonPathValidation = realTimeJSonPathValidation;
    }

    public boolean isRealTimeIdReferenceTypeValidation() {
        return realTimeIdReferenceTypeValidation;
    }

    public void setRealTimeIdReferenceTypeValidation(boolean realTimeIdReferenceTypeValidation) {
        this.realTimeIdReferenceTypeValidation = realTimeIdReferenceTypeValidation;
    }

    public boolean isHighlightCustomOptions() {
        return highlightCustomOptions;
    }

    public void setHighlightCustomOptions(boolean highlightCustomOptions) {
        this.highlightCustomOptions = highlightCustomOptions;
    }

    public boolean isDownloadCatalog() {
        return downloadCatalog;
    }

    public void setDownloadCatalog(boolean downloadCatalog) {
        this.downloadCatalog = downloadCatalog;
    }

    public boolean isScanThirdPartyComponents() {
        return scanThirdPartyComponents;
    }

    public void setScanThirdPartyComponents(boolean scanThirdPartyComponents) {
        this.scanThirdPartyComponents = scanThirdPartyComponents;
    }

    public boolean isShowCamelIconInGutter() {
        return showCamelIconInGutter;
    }

    public void setShowCamelIconInGutter(boolean showCamelIconInGutter) {
        this.showCamelIconInGutter = showCamelIconInGutter;
    }

    public List<String> getIgnorePropertyList() {
        if (ignorePropertyList.isEmpty()) {
            ignorePropertyList = new ArrayList<>(Arrays.asList(DEFAULT_IGNORE_PROPERTIES));
        }
        return ignorePropertyList;
    }

    // called with reflection when loadState is called
    public void setIgnorePropertyList(List<String> ignorePropertyList) {
        this.ignorePropertyList = ignorePropertyList;
    }

    public List<String> getExcludePropertyFiles() {
        if (excludePropertyFiles.isEmpty()) {
            excludePropertyFiles = new ArrayList<>(Arrays.asList(DEFAULT_EXCLUDE_FILE_PATTERN));
        }
        return excludePropertyFiles;
    }

    // called with reflection when loadState is called
    public void setExcludePropertyFiles(List<String> excludePropertyFiles) {
        this.excludePropertyFiles = excludePropertyFiles;
    }

    public Icon getCamelIcon() {
        return CAMEL_ICON;
    }

    @Override
    public void dispose() {
        // noop
    }

    @Override
    public CamelPreferenceService getState() {
        if (ignorePropertyList.isEmpty()) {
            ignorePropertyList = new ArrayList<>(Arrays.asList(DEFAULT_IGNORE_PROPERTIES));
        }
        return this;
    }

    @Override
    public void loadState(@NotNull CamelPreferenceService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelPreferenceService that = (CamelPreferenceService) o;
        return realTimeEndpointValidation == that.realTimeEndpointValidation
            && realTimeSimpleValidation == that.realTimeSimpleValidation
            && realTimeJSonPathValidation == that.realTimeJSonPathValidation
            && realTimeIdReferenceTypeValidation == that.realTimeIdReferenceTypeValidation
            && downloadCatalog == that.downloadCatalog
            && scanThirdPartyComponents == that.scanThirdPartyComponents
            && showCamelIconInGutter == that.showCamelIconInGutter
            && Objects.equals(ignorePropertyList, that.ignorePropertyList)
            && Objects.equals(excludePropertyFiles, that.excludePropertyFiles)
            && Objects.equals(xmlPropertyPlaceholders, that.xmlPropertyPlaceholders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realTimeEndpointValidation, realTimeSimpleValidation, realTimeJSonPathValidation,
            realTimeIdReferenceTypeValidation, downloadCatalog, scanThirdPartyComponents,
            ignorePropertyList, excludePropertyFiles, xmlPropertyPlaceholders);
    }

    public boolean isRealTimeBeanMethodValidationCheckBox() {
        return realTimeBeanMethodValidationCheckBox;
    }

    public void setRealTimeBeanMethodValidationCheckBox(boolean realTimeBeanMethodValidationCheckBox) {
        this.realTimeBeanMethodValidationCheckBox = realTimeBeanMethodValidationCheckBox;
    }

    public List<XmlPropertyPlaceholderDefinition> getEnabledXmlPropertyPlaceholders() {
        return xmlPropertyPlaceholders.stream()
                .filter(PropertyPlaceholderSettingsEntry::isEnabled)
                .map(def -> new XmlPropertyPlaceholderDefinition(def.getStartToken(), def.getEndToken(), def.getNamespaces()))
                .toList();
    }

}
