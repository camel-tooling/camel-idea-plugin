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
package com.github.cameltooling.idea.runner;

import java.util.List;
import java.util.Objects;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

@SuppressWarnings("unchecked")
public class CamelJBangRunConfigurationOptions extends RunConfigurationOptions {

    private final StoredProperty<List<Object>> files = list().provideDelegate(this, "files");
    private final StoredProperty<List<Object>> cmdOptions = list().provideDelegate(this, "cmdOptions");
    private final StoredProperty<List<Object>> dependencies = list().provideDelegate(this, "dependencies");

    public List<String> getFiles() {
        return (List) files.getValue(this);
    }

    public void setFiles(List<String> files) {
        this.files.setValue(this, (List) files);
    }

    public List<String> getCmdOptions() {
        return (List) cmdOptions.getValue(this);
    }

    public void setCmdOptions(List<String> cmdOptions) {
        this.cmdOptions.setValue(this, (List) cmdOptions);
    }

    public List<String> getDependencies() {
        return (List) dependencies.getValue(this);
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies.setValue(this, (List) dependencies);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CamelJBangRunConfigurationOptions that = (CamelJBangRunConfigurationOptions) o;
        return Objects.equals(files, that.files) && Objects.equals(cmdOptions, that.cmdOptions)
            && Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), files, cmdOptions, dependencies);
    }
}
