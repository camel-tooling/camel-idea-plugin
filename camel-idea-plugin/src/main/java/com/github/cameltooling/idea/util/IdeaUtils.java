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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.cameltooling.idea.extension.IdeaUtilsExtension;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.JavaDocElementType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.java.IJavaDocElementType;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

import static com.intellij.xml.CommonXmlStrings.QUOT;

/**
 * Utility methods to work with IDEA {@link PsiElement}s.
 * <p/>
 * This class is only for IDEA APIs. If you need Camel related APIs as well, use {@link CamelIdeaUtils} instead.
 */
@Service
public final class IdeaUtils implements Disposable {

    private static final List<String> ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME = List.of(
        "org.apache.camel.builder.endpoint.EndpointRouteBuilder",
        "org.apache.camel.builder.RouteBuilder",
        "org.apache.camel.builder.BuilderSupport",
        "org.apache.camel.model.ProcessorDefinition",
        "org.apache.camel.model.language.ExpressionDefinition"
    );

    private final List<IdeaUtilsExtension> enabledExtensions;

    private IdeaUtils() {
        enabledExtensions = Arrays.stream(IdeaUtilsExtension.EP_NAME.getExtensions())
            .filter(IdeaUtilsExtension::isExtensionEnabled)
            .toList();
    }

    public static IdeaUtils getService() {
        return ApplicationManager.getApplication().getService(IdeaUtils.class);
    }

    /**
     * Extract the text value from the {@link PsiElement} from any of the support languages this plugin works with.
     *
     * @param element the element
     * @return the text or <tt>null</tt> if the element is not a text/literal kind.
     */
    @Nullable
    public String extractTextFromElement(PsiElement element) {
        return extractTextFromElement(element, true, false, true);
    }

    /**
     * Extract the text value from the {@link PsiElement} from any of the support languages this plugin works with.
     *
     * @param element the element
     * @param fallBackToGeneric if it could find any of the supported language-fallbacks to generic if true
     * @param concatString concatenated the string if it wrapped
     * @param stripWhitespace strip whitespaces
     * @return the text or <tt>null</tt> if the element is not a text/literal kind.
     */
    @Nullable
    public String extractTextFromElement(PsiElement element, boolean fallBackToGeneric, boolean concatString, boolean stripWhitespace) {
        return enabledExtensions.stream()
            .map(extension -> extension.extractTextFromElement(element, concatString, stripWhitespace))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst().orElseGet(() -> {
                if (fallBackToGeneric) {
                    // fallback to generic
                    String text = element.getText();
                    if (concatString) {
                        final PsiPolyadicExpression parentOfType = PsiTreeUtil.getParentOfType(element, PsiPolyadicExpression.class);
                        if (parentOfType != null) {
                            text = parentOfType.getText();
                        }
                    }
                    // the text may be quoted so unwrap that
                    if (stripWhitespace) {
                        return getInnerText(text);
                    }
                    return StringUtil.unquoteString(text.replace(QUOT, "\""));
                }
                return null;
            });
    }

    /**
     * Is the element from a java setter method (e.g., setBrokerURL) or from an XML configured <tt>bean</tt> style
     * configuration using <tt>property</tt> element.
     */
    public boolean isElementFromSetterProperty(@NotNull PsiElement element, @NotNull String setter) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isElementFromSetterProperty(element, setter));
    }

    /**
     * Is the element from a java annotation with the given name.
     */
    public boolean isElementFromAnnotation(@NotNull PsiElement element, @NotNull String annotationName) {
        // java method call
        PsiAnnotation ann = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class, false);
        if (ann != null) {
            return annotationName.equals(ann.getQualifiedName());
        }

        return false;
    }

    /**
     * Is the element from Java language
     */
    public boolean isJavaLanguage(PsiElement element) {
        return element != null && PsiUtilCore.getNotAnyLanguage(element.getNode()).is(JavaLanguage.INSTANCE);
    }

    /**
     * Is the element from YAML language
     */
    public boolean isYamlLanguage(PsiElement element) {
        return element != null && PsiUtilCore.getNotAnyLanguage(element.getNode()).is(YAMLLanguage.INSTANCE);
    }

    /**
     * Is the element from a file of the given extensions such as <tt>java</tt>, <tt>xml</tt>, etc.
     */
    public boolean isFromFileType(PsiElement element, @NotNull String... extensions) {
        if (extensions.length == 0) {
            throw new IllegalArgumentException("Extension must be provided");
        }

        PsiFile file;
        if (element instanceof PsiFile psiFile) {
            file = psiFile;
        } else {
            file = PsiTreeUtil.getParentOfType(element, PsiFile.class);
        }
        if (file != null) {
            String name = file.getName().toLowerCase();
            for (String match : extensions) {
                if (name.endsWith("." + match.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a URLClassLoader for a given library or libraries
     *
     * @param libraries the library or libraries
     * @return the classloader
     */
    public @Nullable URLClassLoader newURLClassLoaderForLibrary(Library... libraries) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (Library library : libraries) {
            if (library == null) {
                continue;
            }
            VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
            if (files.length == 1) {
                VirtualFile vf = files[0];
                if (vf.getName().toLowerCase().endsWith(".jar")) {
                    String path = vf.getPath();
                    if (path.endsWith("!/")) {
                        path = path.substring(0, path.length() - 2);
                    }
                    urls.add(new URL("file:" + path));
                }
            }
        }
        if (urls.isEmpty()) {
            return null;
        }

        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    /**
     * Is the given class or any of its superclasses a class with the qualified name.
     *
     * @param target  the class
     * @param fqnClassName the class name to match
     * @return <tt>true</tt> if the class is a type or subtype of the class name
     */
    private static boolean isClassOrParentOf(@Nullable PsiClass target, @NotNull String fqnClassName) {
        if (target == null) {
            return false;
        }
        if (Objects.equals(target.getQualifiedName(), fqnClassName)) {
            return true;
        } else {
            return isClassOrParentOf(target.getSuperClass(), fqnClassName);
        }
    }

    /**
     * Is the element from a constructor call with the given constructor name (e.g., class name)
     *
     * @param element  the element
     * @param constructorName the name of the constructor (e.g., class)
     * @return <tt>true</tt> if it is a constructor call from the given name, <tt>false</tt> otherwise
     */
    public boolean isElementFromConstructor(@NotNull PsiElement element, @NotNull String constructorName) {
        // java constructor
        PsiConstructorCall call = PsiTreeUtil.getParentOfType(element, PsiConstructorCall.class);
        if (call != null) {
            PsiMethod resolved = call.resolveConstructor();
            if (resolved != null) {
                return constructorName.equals(resolved.getName());
            }
        }
        return false;
    }

    /**
     * Is the given element from a Java method call with any of the given method names?
     *
     * @param element  the psi element
     * @param methods  method call names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean isFromJavaMethodCall(PsiElement element, boolean fromRouteBuilder, String... methods) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            return isFromJavaMethod(call, fromRouteBuilder, methods);
        }
        return false;
    }

    /**
     * Returns the first parent of the given element which matches the given condition.
     *
     * @param element element from which the search starts
     * @param strict if true, the element itself cannot be returned if it matches the condition
     * @param matchCondition condition which the parent must match to be returned
     * @param stopCondition condition which stops the search, causing the method to return null
     */
    public PsiElement findFirstParent(@Nullable PsiElement element,
                                      boolean strict,
                                      Predicate<? super PsiElement> matchCondition,
                                      Predicate<? super PsiElement> stopCondition) {
        PsiElement parent = PsiTreeUtil.findFirstParent(element, strict, e -> stopCondition.test(e) || matchCondition.test(e));
        if (parent != null && matchCondition.test(parent)) {
            return parent;
        } else {
            return null;
        }
    }

    public boolean isFromJavaMethod(PsiMethodCallExpression call, boolean fromRouteBuilder, String... methods) {
        PsiMethod method = call.resolveMethod();
        if (method != null) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                String name = method.getName();
                // TODO: this code should likely be moved to something that requires it from being a Camel RouteBuilder
                if (Arrays.asList(methods).contains(name)) {
                    if (fromRouteBuilder) {
                        return ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME.stream().anyMatch(t -> isClassOrParentOf(containingClass, t));
                    } else {
                        return true;
                    }
                }
            }
        } else {
            // TODO : This should be removed when we figure how to setup language depend SDK classes
            // alternative when we run unit test where IDEA causes the method call expression to include their dummy hack which skews up this logic
            PsiElement child = call.getFirstChild();
            if (child != null) {
                child = child.getLastChild();
            }
            if (child instanceof PsiIdentifier) {
                String name = child.getText();
                return Arrays.asList(methods).contains(name);
            }
        }
        return false;
    }

    /**
     * Indicates whether the given YAML key-value pair matches with the uri of one of the given {@code eips}.
     *
     * @param keyValue  the YAML key-value pair to test
     * @param eips  the name of the EIP to test
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean isURIYAMLKeyValue(@NotNull YAMLKeyValue keyValue, @NotNull String... eips) {
        final String key = keyValue.getKeyText();
        if (key.equals("uri") || key.equals("dead-letter-uri")) {
            final YAMLKeyValue parent = PsiTreeUtil.getParentOfType(keyValue, YAMLKeyValue.class);
            return parent != null && Arrays.asList(eips).contains(parent.getKeyText());
        }
        return Arrays.asList(eips).contains(keyValue.getKeyText()) && keyValue.getValue() != null;
    }

    /**
     * Indicates whether the given YAML key-value pair has for parent one of the given {@code eips}.
     *
     * @param keyValue  the YAML key-value pair to test
     * @param eips  the name of the EIP to test
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean hasParentYAMLKeyValue(@NotNull YAMLKeyValue keyValue, @NotNull String... eips) {
        YAMLKeyValue parent = PsiTreeUtil.getParentOfType(keyValue, YAMLKeyValue.class);
        if (parent == null) {
            return false;
        } else if (Arrays.asList(eips).contains(parent.getKeyText())) {
            return true;
        }
        parent = PsiTreeUtil.getParentOfType(keyValue, YAMLKeyValue.class);
        return parent != null && Arrays.asList(eips).contains(parent.getKeyText());
    }

    /**
     * Is the given element from an XML tag with the parent and is of any given tag names?
     *
     * @param xml  the xml tag
     * @param parentTag a special parent tag name to match first
     * @param methods  xml tag names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */

    /**
     * Calls the given consumer for each Yaml file that could be found in the given module.
     * @param module the module in which Yaml files should be found.
     * @param yamlFileConsumer the consumer to call anytime a Yaml has been found.
     */
    public void iterateYamlFiles(Module module, Consumer<YAMLFile> yamlFileConsumer) {
        final GlobalSearchScope moduleScope = module.getModuleContentScope();
        final GlobalSearchScope yamlFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(moduleScope, YAMLFileType.YML);

        ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        fileIndex.iterateContent(f -> {
            if (yamlFiles.contains(f)) {
                PsiFile file = PsiManager.getInstance(module.getProject()).findFile(f);
                if (file instanceof YAMLFile yamlFile) {
                    yamlFileConsumer.accept(yamlFile);
                }
            }
            return true;
        });
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    public String getInnerText(String text) {
        if (text == null) {
            return null;
        }
        if (StringUtil.endsWithChar(text, '\"') && text.length() == 1) {
            return "";
        }
        // Remove any newline feed + whitespaces + single + double quot to concat a split string
        return StringUtil.unquoteString(text.replace(QUOT, "\"")).replaceAll("(^\\n\\s+|\\n\\s+$|\\n\\s+)|(\"\\s*\\+\\s*\")|(\"\\s*\\+\\s*\\n\\s*\"*)", "");
    }

    private int getCaretPositionInsidePsiElement(String stringLiteral) {
        String hackVal = stringLiteral.toLowerCase();

        int hackIndex = hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER.toLowerCase());
        if (hackIndex == -1) {
            hackIndex = hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.toLowerCase());
        }
        return hackIndex;
    }

    /**
     * Return the Query parameter at the cursor location for the query parameter.
     *  <ul>
     *    <li>timer:trigger?repeatCount=0&de&lt;cursor&gt; will return {"&de", null}</li>
     *    <li>timer:trigger?repeatCount=0&de&lt;cursor&gt;lay=10 will return {"&de",null}</li>
     *    <li>timer:trigger?repeatCount=0&delay=10&lt;cursor&gt; will return {"delay","10"}</li>
     *    <li>timer:trigger?repeatCount=0&delay=&lt;cursor&gt; will return {"delay",""}</li>
     *    <li>jms:qu&lt;cursor&gt; will return {":qu", ""}</li>
     *  </ul>
     * @return an array with the query parameter and the value if present. The query parameter is returned with separator char
     */
    public String[] getQueryParameterAtCursorPosition(PsiElement element) {
        String positionText = extractTextFromElement(element);
        positionText = positionText.replaceAll("&amp;", "&");

        int hackIndex = getCaretPositionInsidePsiElement(positionText);
        positionText = positionText.substring(0, hackIndex);
        //we need to know the start position of the unknown options
        int startIdx = Math.max(positionText.lastIndexOf('.'), positionText.lastIndexOf('='));
        startIdx = Math.max(startIdx, positionText.lastIndexOf('&'));
        startIdx = Math.max(startIdx, positionText.lastIndexOf('?'));
        startIdx = Math.max(startIdx, positionText.lastIndexOf(':'));

        startIdx = Math.max(startIdx, 0);

        //Copy the option with any separator chars
        String parameter;
        String value = null;
        if (!positionText.isEmpty() && positionText.charAt(startIdx) == '=') {
            value = positionText.substring(startIdx + 1, hackIndex);
            int valueStartIdx = positionText.lastIndexOf('&', startIdx);
            valueStartIdx = Math.max(valueStartIdx, positionText.lastIndexOf('?'));
            valueStartIdx = Math.max(valueStartIdx, positionText.lastIndexOf(':'));
            valueStartIdx = Math.max(valueStartIdx, 0);
            parameter = positionText.substring(valueStartIdx, startIdx);
        } else {
            //Copy the option with any separator chars
            parameter = positionText.substring(startIdx, hackIndex);
        }

        return new String[]{parameter, value};
    }

    public boolean isCaretAtEndOfLine(PsiElement element) {
        String value = extractTextFromElement(element).trim();

        value = value.toLowerCase();
        return value.endsWith(CompletionUtil.DUMMY_IDENTIFIER.toLowerCase())
            || value.endsWith(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.toLowerCase());
    }

    public boolean isWhiteSpace(PsiElement element) {
        IElementType type = element.getNode().getElementType();
        return type == TokenType.WHITE_SPACE;
    }

    public boolean isJavaDoc(PsiElement element) {
        IElementType type = element.getNode().getElementType();
        return IJavaDocElementType.class.isAssignableFrom(type.getClass())
                || JavaDocElementType.ALL_JAVADOC_ELEMENTS.contains(element.getNode().getElementType());
    }

    public Optional<XmlAttribute> findAttribute(XmlTag tag, String localName) {
        return Arrays.stream(tag.getAttributes())
            .filter(a -> a.getLocalName().equals(localName))
            .findAny();
    }

    public Optional<XmlAttributeValue> findAttributeValue(XmlTag tag, String localName) {
        return findAttribute(tag, localName)
            .map(XmlAttribute::getValueElement);
    }

    public TextRange getUnquotedRange(PsiElement element) {
        TextRange originalRange = element.getTextRange();
        if (StringUtil.isQuotedString(element.getText())) {
            return TextRange.create(originalRange.getStartOffset() + 1, originalRange.getEndOffset() - 1);
        } else {
            return originalRange;
        }
    }

    public PsiType findAnnotatedElementType(PsiAnnotation annotation) {
        PsiField field = PsiTreeUtil.getParentOfType(annotation, PsiField.class);
        if (field != null) {
            return field.getType();
        } else {
            PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
            if (method != null && method.getParameterList().getParametersCount() == 1) {
                return method.getParameterList().getParameters()[0].getType();
            }
            return null;
        }
    }

    @Override
    public void dispose() {
        //noop
    }

    public static YAMLKeyValue getYamlKeyValueAt(Project project, XSourcePosition position) {
        VirtualFile file = position.getFile();
        YAMLKeyValue keyValue = null;
        PsiElement psiElement = XDebuggerUtil.getInstance().findContextElement(file, position.getOffset(), project, false);

        // This must be the indent element because the position is at the beginning of the line
        if (psiElement instanceof LeafPsiElement leafPsiElement && "indent".equals(leafPsiElement.getElementType().toString())) {
            psiElement = psiElement.getNextSibling(); //This must be the sequence item
            Collection<YAMLKeyValue> keyValues = null;
            if (psiElement instanceof YAMLSequence yamlSequence) {
                // This is the beginning of the sequence; get the first item
                psiElement = yamlSequence.getItems().get(0);
                keyValues = ((YAMLSequenceItem) psiElement).getKeysValues();
            } else if (psiElement instanceof YAMLSequenceItem yamlSequenceItem) {
                keyValues = yamlSequenceItem.getKeysValues();
            } else if (psiElement instanceof YAMLKeyValue yamlKeyValue) {
                keyValue = yamlKeyValue;
            } else if (psiElement instanceof YAMLMapping yamlMapping) {
                keyValues = yamlMapping.getKeyValues();
            }
            if (keyValues != null && !keyValues.isEmpty()) {
                keyValue = keyValues.iterator().next();
            }
        }

        return keyValue;
    }

    public static PsiClass findRouteBuilderClass(PsiManager manager) {
        return ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME
                .stream()
                .map(fqn -> ClassUtil.findPsiClass(manager, fqn))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
