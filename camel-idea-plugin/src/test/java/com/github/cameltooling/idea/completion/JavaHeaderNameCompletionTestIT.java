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
package com.github.cameltooling.idea.completion;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.testFramework.PsiTestUtil;

/**
 * Testing the completion of the header names based on the headers defined in the metadata of a component.
 */
public class JavaHeaderNameCompletionTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static String CAMEL_CORE_MODEL_MAVEN_ARTIFACT = "org.apache.camel:camel-core-model:%s";
    private static String CAMEL_FILE_MAVEN_ARTIFACT = "org.apache.camel:camel-file:%s";

    private static final File[] mavenArtifacts;

    static {
        try {
            CAMEL_CORE_MODEL_MAVEN_ARTIFACT = String.format(CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_VERSION);
            CAMEL_FILE_MAVEN_ARTIFACT = String.format(CAMEL_FILE_MAVEN_ARTIFACT, CAMEL_VERSION);
            mavenArtifacts = getMavenArtifacts(CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_FILE_MAVEN_ARTIFACT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        PsiTestUtil.addLibrary(myFixture.getProjectDisposable(), myFixture.getModule(), "Maven: " + CAMEL_CORE_MODEL_MAVEN_ARTIFACT, mavenArtifacts[0].getParent(), mavenArtifacts[0].getName());
        PsiTestUtil.addLibrary(myFixture.getProjectDisposable(), myFixture.getModule(), "Maven: " + CAMEL_FILE_MAVEN_ARTIFACT, mavenArtifacts[1].getParent(), mavenArtifacts[1].getName());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/header";
    }

    /**
     * Ensures that suggestions of a single endpoint can be found.
     */
    public void testSingleEndpointSuggestions() {
        for (TestType type : TestType.values()) {
            testSingleEndpointSuggestions(type);
        }
    }

    private void testSingleEndpointSuggestions(TestType type) {
        myFixture.configureByFiles(type.getFilePath("SingleEndpointSuggestions"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertNotEmpty(
            strings.stream()
            .filter(s -> type.isFileHeader(s) && type.accept(s))
            .collect(Collectors.toList())
        );
        assertEmpty(
            strings.stream()
            .filter(type::isFtpHeader)
            .collect(Collectors.toList())
        );
    }

    /**
     * Ensures that suggestions of a multiple endpoints can be found.
     */
    public void testMultipleEndpointSuggestions() {
        for (TestType type : TestType.values()) {
            testMultipleEndpointSuggestions(type);
        }
    }

    private void testMultipleEndpointSuggestions(TestType type) {
        myFixture.configureByFiles(type.getFilePath("MultipleEndpointSuggestions"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertNotEmpty(
            strings.stream()
                .filter(s -> type.isFileHeader(s) && type.accept(s))
                .collect(Collectors.toList())
        );
        assertNotEmpty(
            strings.stream()
                .filter(s -> type.isFtpHeader(s) && type.accept(s))
                .collect(Collectors.toList())
        );
    }

    /**
     * Ensures that suggestions can be filtered out by typing first letters.
     */
    public void testFilteringOutSuggestions() {
        for (TestType type : TestType.values()) {
            testFilteringOutSuggestions(type);
        }
    }

    private void testFilteringOutSuggestions(TestType type) {
        myFixture.configureByFiles(type.getFilePath("MultipleEndpointSuggestions"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        List<String> before = strings.stream()
            .filter(s -> type.isFtpHeader(s) && type.accept(s))
            .collect(Collectors.toList());
        myFixture.type("Ftp");
        strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertEmpty(
            strings.stream()
                .filter(s -> type.isFileHeader(s) && type.accept(s))
                .collect(Collectors.toList())
        );
        List<String> after = strings.stream()
            .filter(s -> type.isFtpHeader(s) && type.accept(s))
            .collect(Collectors.toList());
        assertSameElements(after, before);
    }

    /**
     * Ensures that suggestions can be filtered out in a String literal by typing first letters.
     */
    public void testEndpointSuggestionsInLiteral() {
        for (TestType type : TestType.values()) {
            if (type.literalSupport()) {
                testEndpointSuggestionsInLiteral(type);
            }
        }
    }

    private void testEndpointSuggestionsInLiteral(TestType type) {
        myFixture.configureByFiles(type.getFilePath("EndpointSuggestionsInLiteral"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        List<String> before = strings.stream()
            .filter(s -> s.startsWith("CamelFile") && !s.contains(","))
            .collect(Collectors.toList());
        myFixture.type("CamelFileN");
        strings = myFixture.getLookupElementStrings();
        List<String> after = strings.stream()
            .filter(s -> s.startsWith("CamelFile") && !s.contains(","))
            .collect(Collectors.toList());
        assertTrue("The result size should have reduced", before.size() > after.size());
        assertFalse("Only suggestion starting with CamelFileN should be found", after.stream().anyMatch(s -> !s.startsWith("CamelFileN")));
    }

    /**
     * Ensures that suggestions can be filtered out by typing first letters.
     */
    public void testFilteringOutSuggestionsInLiteral() {
        for (TestType type : TestType.values()) {
            if (type.literalSupport()) {
                testFilteringOutSuggestionsInLiteral(type);
            }
        }
    }

    private void testFilteringOutSuggestionsInLiteral(TestType type) {
        myFixture.configureByFiles(type.getFilePath("EndpointSuggestionsInLiteral"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertNotEmpty(
            strings.stream()
                .filter(s -> s.startsWith("CamelFile") && !s.contains(","))
                .collect(Collectors.toList())
        );
    }

    /**
     * Ensures that selecting a suggestion will auto import the class if known and use the simple class name.
     */
    public void testHeaderCompletionOnKnownComponent() {
        for (TestType type : TestType.values()) {
            if (type.importSupport()) {
                testHeaderCompletionOnKnownComponent(type);
            }
        }
    }

    private void testHeaderCompletionOnKnownComponent(TestType type) {
        myFixture.configureByFiles(type.getFilePath("SingleEndpointSuggestions"));
        myFixture.completeBasic();
        myFixture.type('\n');
        myFixture.checkResultByFile(type.getFilePath("HeaderCompletionOnKnownComponent"));
    }

    /**
     * Ensures that selecting a suggestion will use the full qualified class name if unknown.
     */
    public void testHeaderCompletionOnUnKnownComponent() {
        for (TestType type : TestType.values()) {
            if (type.importSupport()) {
                testHeaderCompletionOnUnKnownComponent(type);
            }
        }
    }

    private void testHeaderCompletionOnUnKnownComponent(TestType type) {
        myFixture.configureByFiles(type.getFilePath("MultipleEndpointSuggestions"));
        myFixture.completeBasic();
        myFixture.type("Ftp\n");
        myFixture.checkResultByFile(type.getFilePath("HeaderCompletionOnUnknownComponent"));
    }

    enum TestType {
        SET_HEADER_JAVA("set", FileType.JAVA) {
            @Override
            public boolean accept(String suggestion) {
                return suggestion.contains(",");
            }
        },
        HEADER_JAVA("get", FileType.JAVA) {
            @Override
            public boolean accept(String suggestion) {
                return !suggestion.contains(",");
            }
        },
        SET_HEADER_XML("set", FileType.XML) {
            @Override
            public boolean accept(String suggestion) {
                return !suggestion.contains(",");
            }
        },
        HEADER_XML("get", FileType.XML) {
            @Override
            public boolean accept(String suggestion) {
                return !suggestion.contains(",");
            }
        },
        SET_HEADER_YAML("set", FileType.YAML) {
            @Override
            public boolean accept(String suggestion) {
                return !suggestion.contains(",");
            }
        },
        HEADER_YAML("get", FileType.YAML) {
            @Override
            public boolean accept(String suggestion) {
                return !suggestion.contains(",");
            }
        };

        private final String folder;
        private final FileType fileType;

        TestType(String folder, FileType fileType) {
            this.folder = folder;
            this.fileType = fileType;
        }

        public String getFilePath(String fileName) {
            return String.format("%s/%s.%s", folder, fileName, fileType.getExtension());
        }

        public abstract boolean accept(String suggestion);

        public boolean literalSupport() {
            return fileType.literalSupport;
        }

        public boolean importSupport() {
            return fileType.importSupport;
        }

        public boolean isFileHeader(String suggestion) {
            return fileType.isFileHeader(suggestion);
        }

        public boolean isFtpHeader(String suggestion) {
            return fileType.isFtpHeader(suggestion);
        }
    }
    enum FileType {
        JAVA(true, true) {
            @Override
            public boolean isFileHeader(String suggestion) {
                return suggestion.startsWith("FileConstants.");
            }

            @Override
            public boolean isFtpHeader(String suggestion) {
                return suggestion.startsWith("FtpConstants.");
            }
        },
        XML(false, false) {
            @Override
            public boolean isFileHeader(String suggestion) {
                return suggestion.startsWith("CamelFile");
            }

            @Override
            public boolean isFtpHeader(String suggestion) {
                return suggestion.startsWith("CamelFtp");
            }
        },
        YAML(false, false) {
            @Override
            public boolean isFileHeader(String suggestion) {
                return suggestion.startsWith("CamelFile");
            }

            @Override
            public boolean isFtpHeader(String suggestion) {
                return suggestion.startsWith("CamelFtp");
            }
        };

        private final boolean literalSupport;
        private final boolean importSupport;

        FileType(boolean literalSupport, boolean importSupport) {
            this.literalSupport = literalSupport;
            this.importSupport = importSupport;
        }

        String getExtension() {
            return name().toLowerCase();
        }

        public abstract boolean isFileHeader(String suggestion);

        public abstract boolean isFtpHeader(String suggestion);
    }
}
