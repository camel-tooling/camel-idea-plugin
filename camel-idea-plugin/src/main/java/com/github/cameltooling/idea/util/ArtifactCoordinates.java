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

import com.intellij.openapi.roots.LibraryOrderEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifies the coordinates of a given artifact.
 */
public final class ArtifactCoordinates {
    /**
     * The id of the group of the artifact.
     */
    private final String groupId;
    /**
     * The id of the artifact.
     */
    private final String artifactId;
    /**
     * The version of the artifact.
     */
    private final String version;

    /**
     * Construct a {@code ArtifactCoordinates} with the given parameters.
     * @param groupId the id of the group of the artifact.
     * @param artifactId the id of the artifact.
     * @param version the version of the artifact.
     */
    private ArtifactCoordinates(@NotNull String groupId, @NotNull String artifactId, @Nullable String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Parses the user-visible name of the given {@link LibraryOrderEntry} to extract the coordinates of the
     * corresponding artifact.
     *
     * @param libraryOrderEntry the entry to convert into {@code ArtifactCoordinates}.
     * @return a new instance of {@code ArtifactCoordinates} corresponding to what could be extracted from user-visible
     * name of the given {@link LibraryOrderEntry} if possible, otherwise it is processed as a custom library.
     */
    @NotNull
    public static ArtifactCoordinates parse(LibraryOrderEntry libraryOrderEntry) {
        String presentableName = libraryOrderEntry.getPresentableName().toLowerCase();
        String[] split = presentableName.split(":");
        if (split.length < 3) {
            return new ArtifactCoordinates("$", presentableName, null);
        }
        int startIdx = 0;
        if (split[0].equalsIgnoreCase("maven")
            || split[0].equalsIgnoreCase("gradle")
            || split[0].equalsIgnoreCase("sbt")) {
            startIdx = 1;
        }
        boolean hasVersion = split.length > (startIdx + 2);

        String groupId = split[startIdx++].trim();
        String artifactId = split[startIdx++].trim();
        String version = null;
        if (hasVersion) {
            version = split[startIdx].trim();
            // adjust snapshot which must be in uppercase
            version = version.replace("snapshot", "SNAPSHOT");
        }
        return new ArtifactCoordinates(groupId, artifactId, version);
    }

    /**
     * @return the id of the group of the artifact.
     */
    @NotNull
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return the id of the artifact.
     */
    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return the version of the artifact.
     */
    @Nullable
    public String getVersion() {
        return version;
    }
}
