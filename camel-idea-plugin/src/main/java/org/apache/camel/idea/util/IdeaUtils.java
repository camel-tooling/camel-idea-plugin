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
package org.apache.camel.idea.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlTag;
import org.apache.camel.idea.extension.IdeaUtilsExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

/**
 * Utility methods to work with IDEA {@link PsiElement}s.
 * <p/>
 * This class is only for IDEA APIs. If you need Camel related APIs as well then use {@link CamelIdeaUtils} instead.
 */
public final class IdeaUtils implements Disposable {

    private static final List<String> ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME = Arrays.asList(
        "org.apache.camel.builder.RouteBuilder", "org.apache.camel.builder.BuilderSupport",
        "org.apache.camel.model.ProcessorDefinition", "org.apache.camel.model.language.ExpressionDefinition");

    private final List<IdeaUtilsExtension> enabledExtensions;

    private IdeaUtils() {
        enabledExtensions = Arrays.stream(IdeaUtilsExtension.EP_NAME.getExtensions())
            .filter(IdeaUtilsExtension::isExtensionEnabled)
            .filter(e -> e.isExtensionEnabled())
            .collect(Collectors.toList());
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
     * @param fallBackToGeneric if could find any of the supported languages fallback to generic if true
     * @param concatString concatenated the string if it wrapped
     * @param stripWhitespace
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
     * Is the element from a java setter method (eg setBrokerURL) or from a XML configured <tt>bean</tt> style
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
        return element != null && PsiUtil.getNotAnyLanguage(element.getNode()).is(JavaLanguage.INSTANCE);
    }

    /**
     * Is the element from XML language
     */
    public boolean isXmlLanguage(PsiElement element) {
        return element != null && PsiUtil.getNotAnyLanguage(element.getNode()).is(XMLLanguage.INSTANCE);
    }

    /**
     * Is the element from a file of the given extensions such as <tt>java</tt>, <tt>xml</tt>, etc.
     */
    public boolean isFromFileType(PsiElement element, @NotNull String... extensions) {
        if (extensions.length == 0) {
            throw new IllegalArgumentException("Extension must be provided");
        }

        PsiFile file;
        if (element instanceof PsiFile) {
            file = (PsiFile) element;
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
            if (library != null) {
                VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
                if (files.length == 1) {
                    VirtualFile vf = files[0];
                    if (vf.getName().toLowerCase().endsWith(".jar")) {
                        String path = vf.getPath();
                        if (path.endsWith("!/")) {
                            path = path.substring(0, path.length() - 2);
                        }
                        URL url = new URL("file:" + path);
                        urls.add(url);
                    }
                }
            }
        }
        if (urls.isEmpty()) {
            return null;
        }

        URL[] array = urls.toArray(new URL[urls.size()]);
        return new URLClassLoader(array);
    }

    /**
     * Is the given class or any of its super classes a class with the qualified name.
     *
     * @param target  the class
     * @param fqnClassName the class name to match
     * @return <tt>true</tt> if the class is a type or subtype of the class name
     */
    private static boolean isClassOrParentOf(@Nullable PsiClass target, @NotNull String fqnClassName) {
        if (target == null) {
            return false;
        }
        if (target.getQualifiedName().equals(fqnClassName)) {
            return true;
        } else {
            return isClassOrParentOf(target.getSuperClass(), fqnClassName);
        }
    }

    /**
     * Is the element from a constructor call with the given constructor name (eg class name)
     *
     * @param element  the element
     * @param constructorName the name of the constructor (eg class)
     * @return <tt>true</tt> if its a constructor call from the given name, <tt>false</tt> otherwise
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
     * Is the given element from a Java method call with any of the given method names
     *
     * @param element  the psi element
     * @param methods  method call names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean isFromJavaMethodCall(PsiElement element, boolean fromRouteBuilder, String... methods) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            return doIsFromJavaMethod(call, fromRouteBuilder, methods);
        }
        return false;
    }

    private boolean doIsFromJavaMethod(PsiMethodCallExpression call, boolean fromRouteBuilder, String... methods) {
        PsiMethod method = call.resolveMethod();
        if (method != null) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                String name = method.getName();
                // TODO: this code should likely be moved to something that requires it from being a Camel RouteBuilder
                if (Arrays.stream(methods).anyMatch(name::equals)) {
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
            if (child != null && child instanceof PsiIdentifier) {
                String name = child.getText();
                return Arrays.stream(methods).anyMatch(name::equals);
            }
        }
        return false;
    }

    /**
     * Is the given element from a XML tag with any of the given tag names
     *
     * @param xml  the xml tag
     * @param methods  xml tag names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean isFromXmlTag(@NotNull XmlTag xml, @NotNull String... methods) {
        String name = xml.getLocalName();
        return Arrays.stream(methods).anyMatch(name::equals);
    }

    /**
     * Is the given element from a XML tag with any of the given tag names
     *
     * @param xml  the xml tag
     * @param parentTag a special parent tag name to match first
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean hasParentXmlTag(@NotNull XmlTag xml, @NotNull String parentTag) {
        XmlTag parent = xml.getParentTag();
        return parent != null && parent.getLocalName().equals(parentTag);
    }

    /**
     * Is the given element from a XML tag with the parent and is of any of the given tag names
     *
     * @param xml  the xml tag
     * @param parentTag a special parent tag name to match first
     * @param methods  xml tag names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean hasParentAndFromXmlTag(@NotNull XmlTag xml, @NotNull String parentTag, @NotNull String... methods) {
        return hasParentXmlTag(xml, parentTag) && isFromFileType(xml, methods);
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
     * @return a list with the query parameter and the value if present. The query parameter is returned with separator char
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

        startIdx = startIdx < 0 ? 0 : startIdx;

        //Copy the option with any separator chars
        String parameter;
        String value = null;
        if (!positionText.isEmpty() && positionText.charAt(startIdx) == '=') {
            value = positionText.substring(startIdx + 1, hackIndex);
            int valueStartIdx = positionText.lastIndexOf('&', startIdx);
            valueStartIdx = Math.max(valueStartIdx, positionText.lastIndexOf('?'));
            valueStartIdx = Math.max(valueStartIdx, positionText.lastIndexOf(':'));
            valueStartIdx = valueStartIdx < 0 ? 0 : valueStartIdx;
            parameter = positionText.substring(valueStartIdx, startIdx);
        } else {
            //Copy the option with any separator chars
            parameter = positionText.substring(startIdx, hackIndex);
        }

        return new String[]{parameter, value};
    }

    public boolean isCaretAtEndOfLine(PsiElement element) {
        String value = extractTextFromElement(element).trim();

        if (value != null) {
            value = value.toLowerCase();
            return value.endsWith(CompletionUtil.DUMMY_IDENTIFIER.toLowerCase())
                || value.endsWith(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.toLowerCase());
        }

        return false;
    }

    @Override
    public void dispose() {
        //noop
    }
}
