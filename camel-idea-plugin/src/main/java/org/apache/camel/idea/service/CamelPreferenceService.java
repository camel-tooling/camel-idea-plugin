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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Service for holding preference for this plugin.
 */
@State(
    name = "CamelPreferences",
    storages = {@Storage("apachecamelplugin.xml")})
public final class CamelPreferenceService implements PersistentStateComponent<CamelPreferenceService>, Disposable {

    @Transient
    public static final Icon CAMEL_ICON = IconLoader.getIcon("/icons/camel.png");
    @Transient
    public static final Icon CAMEL_BADGE_ICON = IconLoader.getIcon("/icons/camel-badge.png");
    @Transient
    private static final Logger LOG = Logger.getInstance(CamelPreferenceService.class);
    @Transient
    private static String[] defaultIgnoreProperties = {
        // ignore java and logger prefixes
        "java.", "Logger.", "logger", "appender.", "rootLogger.",
        // ignore camel-spring-boot auto configuration prefixes
        "camel.springboot.", "camel.component.", "camel.dataformat.", "camel.language."};

    @Transient
    private static String[] defaultExcludeFilePattern = {
        "**/log4j.properties", "**/log4j2.properties", "**/logging.properties"};

    private volatile Icon currentCustomIcon;
    private volatile String currentCustomIconPath;

    private boolean realTimeEndpointValidation = true;
    private boolean realTimeSimpleValidation = true;
    private boolean highlightCustomOptions = true;
    private boolean downloadCatalog = true;
    private boolean scanThirdPartyComponents = true;
    private boolean scanThirdPartyLegacyComponents = true;
    private boolean showCamelIconInGutter = true;
    @Transient
    private String chosenCamelIcon = "Camel Icon";
    private String customIconFilePath;
    private List<String> ignorePropertyList = new ArrayList<>();
    private List<String> excludePropertyFiles = new ArrayList<>();

    private CamelPreferenceService() { }

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

    public boolean isScanThirdPartyLegacyComponents() {
        return scanThirdPartyLegacyComponents;
    }

    public void setScanThirdPartyLegacyComponents(boolean scanThirdPartyLegacyComponents) {
        this.scanThirdPartyLegacyComponents = scanThirdPartyLegacyComponents;
    }

    public boolean isShowCamelIconInGutter() {
        return showCamelIconInGutter;
    }

    public void setShowCamelIconInGutter(boolean showCamelIconInGutter) {
        this.showCamelIconInGutter = showCamelIconInGutter;
    }

    public String getChosenCamelIcon() {
        return chosenCamelIcon;
    }

    public void setChosenCamelIcon(String chosenCamelIcon) {
        this.chosenCamelIcon = chosenCamelIcon;
    }

    public String getCustomIconFilePath() {
        return customIconFilePath;
    }

    public void setCustomIconFilePath(String customIconFilePath) {
        this.customIconFilePath = customIconFilePath;
    }

    public List<String> getIgnorePropertyList() {
        if (ignorePropertyList.isEmpty()) {
            ignorePropertyList = new ArrayList<>(Arrays.asList(defaultIgnoreProperties));
        }
        return ignorePropertyList;
    }

    // called with reflection when loadState is called
    public void setIgnorePropertyList(List<String> ignorePropertyList) {
        this.ignorePropertyList = ignorePropertyList;
    }

    public List<String> getExcludePropertyFiles() {
        if (excludePropertyFiles.isEmpty()) {
            excludePropertyFiles = new ArrayList<>(Arrays.asList(defaultExcludeFilePattern));
        }
        return excludePropertyFiles;
    }

    // called with reflection when loadState is called
    public void setExcludePropertyFiles(List<String> excludePropertyFiles) {
        this.excludePropertyFiles = excludePropertyFiles;
    }

    public Icon getCamelIcon() {
        if (chosenCamelIcon.equals("Camel Icon")) {
            return CAMEL_ICON;
        } else if (chosenCamelIcon.equals("Camel Badge Icon")) {
            return CAMEL_BADGE_ICON;
        }

        if (StringUtils.isNotEmpty(customIconFilePath)) {

            // use cached current icon
            if (customIconFilePath.equals(currentCustomIconPath)) {
                return currentCustomIcon;
            }

            Icon icon = IconLoader.findIcon(customIconFilePath);
            if (icon == null) {
                File file = new File(customIconFilePath);
                if (file.exists() && file.isFile()) {
                    try {
                        URL url = new URL("file:" + file.getAbsolutePath());
                        icon = IconLoader.findIcon(url, true);
                    } catch (MalformedURLException e) {
                        LOG.warn("Error loading custom icon", e);
                    }
                }
            }

            if (icon != null) {
                // cache current icon
                currentCustomIcon = icon;
                currentCustomIconPath = customIconFilePath;
                return currentCustomIcon;
            }
        }

        return CAMEL_ICON;
    }

    @Override
    public void dispose() {
        // noop
    }

    @Nullable
    @Override
    public CamelPreferenceService getState() {
        if (ignorePropertyList.isEmpty()) {
            ignorePropertyList = new ArrayList<>(Arrays.asList(defaultIgnoreProperties));
        }
        return this;
    }

    @Override
    public void loadState(CamelPreferenceService state) {
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
            && downloadCatalog == that.downloadCatalog
            && scanThirdPartyComponents == that.scanThirdPartyComponents
            && scanThirdPartyLegacyComponents == that.scanThirdPartyLegacyComponents
            && showCamelIconInGutter == that.showCamelIconInGutter
            && Objects.equals(currentCustomIcon, that.currentCustomIcon)
            && Objects.equals(currentCustomIconPath, that.currentCustomIconPath)
            && Objects.equals(chosenCamelIcon, that.chosenCamelIcon)
            && Objects.equals(customIconFilePath, that.customIconFilePath)
            && Objects.equals(ignorePropertyList, that.ignorePropertyList)
            && Objects.equals(excludePropertyFiles, that.excludePropertyFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCustomIcon, currentCustomIconPath,
            realTimeEndpointValidation, realTimeSimpleValidation,
            downloadCatalog, scanThirdPartyComponents,
            scanThirdPartyLegacyComponents, showCamelIconInGutter,
            chosenCamelIcon, customIconFilePath, ignorePropertyList, excludePropertyFiles);
    }


}
