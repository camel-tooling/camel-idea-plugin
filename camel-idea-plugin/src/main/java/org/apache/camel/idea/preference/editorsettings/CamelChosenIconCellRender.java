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
package org.apache.camel.idea.preference.editorsettings;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.TextAccessor;
import org.apache.camel.idea.util.StringUtils;
import static org.apache.camel.idea.service.CamelPreferenceService.CAMEL_BADGE_ICON;
import static org.apache.camel.idea.service.CamelPreferenceService.CAMEL_ICON;

/**
 * To render a cell with the possible Camel icons
 */
class CamelChosenIconCellRender extends ListCellRendererWrapper<String> {

    private static final Logger LOG = Logger.getInstance(CamelChosenIconCellRender.class);

    private final TextAccessor textAccessor;

    CamelChosenIconCellRender(TextAccessor textAccessor) {
        this.textAccessor = textAccessor;
    }

    @Override
    public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
        if ("Camel Icon".equals(value)) {
            this.setIcon(CAMEL_ICON);
        } else if ("Camel Badge Icon".equals(value)) {
            this.setIcon(CAMEL_BADGE_ICON);
        } else {
            String custom = textAccessor.getText();
            if (StringUtils.isNotEmpty(custom)) {
                File file = new File(custom);
                if (file.exists() && file.isFile()) {
                    try {
                        URL url = new URL("file:" + custom);
                        Icon icon = IconLoader.findIcon(url, true);
                        if (icon != null) {
                            this.setIcon(icon);
                        }
                    } catch (MalformedURLException e) {
                        LOG.warn("Error loading custom icon", e);
                    }
                }
            }
        }
    }
}
