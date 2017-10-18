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
package org.apache.camel.idea.preference.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * Table view model for the property files to exclude list
 */
public class CamelExcludePropertyFileModel implements Cloneable {
    private List<String> filenames;

    public CamelExcludePropertyFileModel(List<String> filenames) {
        this.filenames = filenames;
    }

    public List<String> getFilenames() {
        return filenames;
    }

    public int size() {
        return filenames.size();
    }

    public String get(int rowIndex) {
        return filenames.get(rowIndex);
    }

    public void add(String propertyValue) {
        this.filenames.add(propertyValue);
    }

    public void add(int newIndex, String propertyValue) {
        this.filenames.add(newIndex, propertyValue);
    }

    public String remove(int index) {
        return filenames.remove(index);
    }

    @Override
    protected CamelExcludePropertyFileModel clone() throws CloneNotSupportedException {
        super.clone();
        return new CamelExcludePropertyFileModel(new ArrayList<>(getFilenames()));
    }


}
