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
package org.apache.camel.idea.preference;

import java.util.ArrayList;
import java.util.List;

/**
 * Table view model for the property ignore list
 */
public class CamelIgnorePropertyModel implements Cloneable {
    private List<String> propertyValues;

    public CamelIgnorePropertyModel(List<String> propertyValues) {
        this.propertyValues = propertyValues;
    }

    public List<String> getPropertyValues() {
        return propertyValues;
    }

    public int size() {
        return propertyValues.size();
    }

    public String get(int rowIndex) {
        return propertyValues.get(rowIndex);
    }

    public void add(String propertyValue) {
        this.propertyValues.add(propertyValue);
    }

    public void add(int newIndex, String propertyValue) {
        this.propertyValues.add(newIndex, propertyValue);
    }

    public String remove(int index) {
        return propertyValues.remove(index);
    }

    @Override
    protected CamelIgnorePropertyModel clone() throws CloneNotSupportedException {
        CamelIgnorePropertyModel camelIgnorePropertyModel = new CamelIgnorePropertyModel(new ArrayList<>(getPropertyValues()));
        return camelIgnorePropertyModel;
    }


}
