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

/**
 * a {@code CamelPreferenceService} for testing purpose.
 */
public class CamelPreferenceServiceMock extends CamelPreferenceService {

    /**
     * Flag indicating whether the preference "Download Catalog" should be disabled or not.
     */
    private static boolean DOWNLOAD_CATALOG_DISABLED;

    @Override
    public boolean isDownloadCatalog() {
        if (DOWNLOAD_CATALOG_DISABLED) {
            return false;
        }
        return super.isDownloadCatalog();
    }

    public static void setDownloadCatalogDisabled(boolean downloadCatalogDisabled) {
        DOWNLOAD_CATALOG_DISABLED = downloadCatalogDisabled;
    }
}
