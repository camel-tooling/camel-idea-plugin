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

import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.apache.camel.idea.service.CamelCatalogService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

/**
 * Utility methods to work with IDEA {@link PsiElement}s.
 */
public final class IdeaUtils {

    private static final String SINGLE_QUOT = "'";

    private static final List<String> ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME = Arrays.asList(
        "org.apache.camel.builder.RouteBuilder", "org.apache.camel.builder.BuilderSupport",
        "org.apache.camel.model.ProcessorDefinition", "org.apache.camel.model.language.ExpressionDefinition");

    private IdeaUtils() {
    }

    /**
     * Extract the text value from the {@link PsiElement} from any of the support languages this plugin works with.
     *
     * @param element the element
     * @return the text or <tt>null</tt> if the element is not a text/literal kind.
     */
    @Nullable
    public static String extractTextFromElement(PsiElement element) {
        return extractTextFromElement(element, true);
    }

    /**
     * Extract the text value from the {@link PsiElement} from any of the support languages this plugin works with.
     *
     * @param element the element
     * @param fallBackToGeneric if could find any of the supported languages fallback to generic if true
     * @return the text or <tt>null</tt> if the element is not a text/literal kind.
     */
    @Nullable
    public static String extractTextFromElement(PsiElement element, boolean fallBackToGeneric) {
        // need the entire line so find the literal expression that would hold the entire string (java)
        PsiLiteralExpression literal = null;
        if (element instanceof PsiLiteralExpression) {
            literal = (PsiLiteralExpression) element;
        }

        if (literal != null) {
            Object o = literal.getValue();
            String text = o != null ? o.toString() : null;
            // unwrap literal string which can happen in java too
            return getInnerText(text);
        }

        // maybe its xml then try that
        XmlAttributeValue xml = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
        if (element instanceof XmlAttributeValue) {
            return ((XmlAttributeValue)element).getValue();
        }

        // its maybe a property from properties file
        String fqn = element.getClass().getName();
        if (fqn.startsWith("com.intellij.lang.properties.psi.impl.PropertyValue")) {
            // yes we can support this also
            return element.getText();
        }

        // maybe its yaml
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("yaml")) {
                return element.getText();
            }
        }

        // maybe its groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                String text = element.getText();
                // unwrap groovy gstring
                return getInnerText(text);
            }
        }

        // maybe its scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                String text = element.getText();
                // unwrap scala string
                return getInnerText(text);
            }
        }

        // maybe its kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                String text = element.getText();
                // unwrap kotlin string
                return getInnerText(text);
            }
        }

        String text = "";
        if (fallBackToGeneric) {
            // fallback to generic
            text = element.getText();
            // the text may be quoted so unwrap that
            text = getInnerText(text);
        }
        return text;
    }

    /**
     * Is the given element from the start of a Camel route, eg <tt>from</tt>, ot &lt;from&gt;.
     */
    public static boolean isCamelRouteStart(PsiElement element) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            return isFromJavaMethod(call, "from", "fromF");
        }
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            String name = xml.getLocalName();
            XmlTag parentTag = xml.getParentTag();
            if (parentTag != null) {
                return "from".equals(name) && "route".equals(parentTag.getLocalName());
            }
        }
        // groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                return isFromGroovyMethod(element, "from", "fromF");
            }
        }
        // kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                return isFromKotlinMethod(element, "from", "fromF");
            }
        }
        // scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                return isFromScalaMethod(element, "from", "fromF");
            }
        }

        return false;
    }

    /**
     * Is the given element a simple of a Camel route, eg <tt>simple</tt>, ot &lt;simple&gt;.
     */
    public static boolean isCamelRouteSimpleExpression(PsiElement element) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            return isFromJavaMethod(call, "simple");
        }
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            String name = xml.getLocalName();
            XmlTag parentTag = xml.getParentTag();
            if (parentTag != null) {
                return "simple".equals(name) && "simple".equals(parentTag.getLocalName());
            }
        }
        // groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                return isFromGroovyMethod(element, "simple");
            }
        }
        // kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                return isFromKotlinMethod(element, "simple");
            }
        }
        // scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                return isFromScalaMethod(element, "simple");
            }
        }

        return false;
    }

    /**
     * Is the given element from a consumer endpoint used in a route from a <tt>from</tt>, <tt>fromF</tt>,
     * <tt>interceptFrom</tt>, or <tt>pollEnrich</tt> pattern.
     */
    public static boolean isConsumerEndpoint(PsiElement element) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            return isFromJavaMethod(call, "from", "fromF", "interceptFrom", "pollEnrich");
        }
        // annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Consume");
        }
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            return isFromXmlTag(xml, "pollEnrich", "from", "interceptFrom");
        }
        // groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                return isFromGroovyMethod(element, "from", "fromF", "interceptFrom", "pollEnrich");
            }
        }
        // kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                return isFromKotlinMethod(element, "from", "fromF", "interceptFrom", "pollEnrich");
            }
        }
        // scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                return isFromScalaMethod(element, "from", "fromF", "interceptFrom", "pollEnrich");
            }
        }

        return false;
    }

    /**
     * Is the given element from a producer endpoint used in a route from a <tt>to</tt>, <tt>toF</tt>,
     * <tt>interceptSendToEndpoint</tt>, <tt>wireTap</tt>, or <tt>enrich</tt> pattern.
     */
    public static boolean isProducerEndpoint(PsiElement element) {
        // java method call
        PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (call != null) {
            return isFromJavaMethod(call, "to", "toF", "toD", "enrich", "interceptSendToEndpoint", "wireTap", "deadLetterChannel");
        }
        // annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null && annotation.getQualifiedName() != null) {
            return annotation.getQualifiedName().equals("org.apache.camel.Produce");
        }
        // xml
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            return isFromXmlTag(xml, "enrich", "to", "interceptSendToEndpoint", "wireTap", "deadLetterChannel");
        }
        // groovy
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                return isFromGroovyMethod(element, "to", "toF", "toD", "enrich", "interceptSendToEndpoint", "wireTap", "deadLetterChannel");
            }
        }
        // kotlin
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("kotlin")) {
                return isFromKotlinMethod(element, "to", "toF", "toD", "enrich", "interceptSendToEndpoint", "wireTap", "deadLetterChannel");
            }
        }
        // scala
        if (element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Scala")) {
                return isFromScalaMethod(element, "to", "toF", "toD", "enrich", "interceptSendToEndpoint", "wireTap", "deadLetterChannel");
            }
        }

        return false;
    }

    /**
     * Is the class a Camel expression class
     *
     * @param clazz  the class
     * @return <tt>true</tt> if its a Camel expression class, <tt>false</tt> otherwise.
     */
    public static boolean isCamelExpressionOrLanguage(PsiClass clazz) {
        if (clazz == null) {
            return false;
        }
        String fqn = clazz.getQualifiedName();
        if ("org.apache.camel.Expression".equals(fqn)
            || "org.apache.camel.Predicate".equals(fqn)
            || "org.apache.camel.model.language.ExpressionDefinition".equals(fqn)
            || "org.apache.camel.builder.ExpressionClause".equals(fqn)) {
            return true;
        }
        // try implements first
        for (PsiClassType ct : clazz.getImplementsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // then fallback as extends
        for (PsiClassType ct : clazz.getExtendsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // okay then go up and try super
        return isCamelExpressionOrLanguage(clazz.getSuperClass());
    }

    public static boolean isJavaLanguage(PsiElement element) {
        return element != null ? PsiUtil.getNotAnyLanguage(element.getNode()).is(JavaLanguage.INSTANCE) : false;
    }

    public static boolean isScalaLanguage(PsiElement element) {
        return element != null ? PsiUtil.getNotAnyLanguage(element.getNode()).isKindOf("Scala") : false;
    }

    public static boolean isXmlLanguage(PsiElement element) {
        return element != null ? PsiUtil.getNotAnyLanguage(element.getNode()).is(XMLLanguage.INSTANCE) : false;
    }

    /**
     * Validate if the query contain a known camel component
     */
    public static boolean isQueryContainingCamelComponent(Project project, String query) {
        // is this a possible Camel endpoint uri which we know
        if (query != null && !query.isEmpty()) {
            String componentName = StringUtils.asComponentName(query);
            if (componentName != null && ServiceManager.getService(project, CamelCatalogService.class).get().findComponentNames().contains(componentName)) {
                return true;
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
    public static @Nullable URLClassLoader newURLClassLoaderForLibrary(Library... libraries) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (Library library : libraries) {
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
    public static boolean isClassOrParentOf(@Nullable PsiClass target, @NotNull String fqnClassName) {
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
     * Is the given element from a Java method call with any of the given method names
     *
     * @param call  the psi method call
     * @param methods  method call names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    private static boolean isFromJavaMethod(PsiMethodCallExpression call, String... methods) {
        PsiMethod method = call.resolveMethod();
        if (method != null) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                String name = method.getName();
                if (Arrays.stream(methods).anyMatch(name::equals)) {
                    return ROUTE_BUILDER_OR_EXPRESSION_CLASS_QUALIFIED_NAME.stream().anyMatch((t) -> isClassOrParentOf(containingClass, t));
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
     * @param parentTag a special parent tag name to match first
     * @param methods  xml tag names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    private static boolean isFromXmlTag(XmlTag xml, String parentTag, String... methods) {
        String name = xml.getLocalName();
        // special check for enrich/pollEnrich where we add the endpoint on a child node (camel expression)
        XmlTag parent = xml.getParentTag();
        if (parent != null && parent.getLocalName().equals(parentTag)) {
            return true;
        }
        return Arrays.stream(methods).anyMatch(name::equals);
    }

    /**
     * Is the given element from a Groovy method call with any of the given method names
     *
     * @param element  the psi element
     * @param methods  method call names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    private static boolean isFromGroovyMethod(PsiElement element, String... methods) {
        // need to walk a bit into the psi tree to find the element that holds the method call name
        // must be a groovy string kind
        String kind = element.toString();
        if (kind.contains("Gstring")) {
            PsiElement parent = element.getParent();
            if (parent != null) {
                parent = parent.getParent();
            }
            if (parent != null) {
                element = parent.getPrevSibling();
            }
            if (element != null) {
                element = element.getLastChild();
            }
        }
        if (element != null) {
            kind = element.toString();
            // must be an identifier which is part of the method call
            if (kind.contains("identifier")) {
                String name = element.getText();
                boolean match = Arrays.stream(methods).anyMatch(name::equals);
                if (match) {
                    // sanity check that "from" was from a from a method call
                    PsiElement parent = element.getParent();
                    if (parent != null) {
                        parent = parent.getParent();
                    }
                    if (parent != null) {
                        kind = parent.toString();
                    }
                    return kind.contains("Method call");
                }
            }
        }
        return false;
    }

    /**
     * Is the given element from a Scala method call with any of the given method names
     *
     * @param element  the psi element
     * @param methods  method call names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    private static boolean isFromScalaMethod(PsiElement element, String... methods) {
        // need to walk a bit into the psi tree to find the element that holds the method call name
        // (yes we need to go up till 5 levels up to find the method call expression
        String kind = element.toString();
        // must be a string kind
        if (kind.contains("string")) {
            for (int i = 0; i < 5; i++) {
                if (element != null) {
                    kind = element.toString();
                    if ("MethodCall".equals(kind)) {
                        element = element.getFirstChild();
                        if (element != null) {
                            String name = element.getText();
                            return Arrays.stream(methods).anyMatch(name::equals);
                        }
                    }
                    if (element != null) {
                        element = element.getParent();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Is the given element from a Kotlin method call with any of the given method names
     *
     * @param element  the psi element
     * @param methods  method call names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    private static boolean isFromKotlinMethod(PsiElement element, String... methods) {
        // need to walk a bit into the psi tree to find the element that holds the method call name
        // (yes we need to go up till 6 levels up to find the method call expression
        String kind = element.toString();
        // must be a string kind
        if (kind.contains("STRING")) {
            for (int i = 0; i < 6; i++) {
                if (element != null) {
                    kind = element.toString();
                    if ("CALL_EXPRESSION".equals(kind)) {
                        element = element.getFirstChild();
                        if (element != null) {
                            String name = element.getText();
                            return Arrays.stream(methods).anyMatch(name::equals);
                        }
                    }
                    if (element != null) {
                        element = element.getParent();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    private static String getInnerText(String text) {
        if (text == null) {
            return null;
        }
        int textLength = text.length();
        if (StringUtil.endsWithChar(text, '\"')) {
            if (textLength == 1) {
                return "";
            }
            text = text.substring(1, textLength - 1);
        } else {
            if (text.startsWith(QUOT) && text.endsWith(QUOT) && textLength > QUOT.length()) {
                text = text.substring(QUOT.length(), textLength - QUOT.length());
            }
            if (text.startsWith(SINGLE_QUOT) && text.endsWith(SINGLE_QUOT) && textLength > SINGLE_QUOT.length()) {
                text = text.substring(SINGLE_QUOT.length(), textLength - SINGLE_QUOT.length());
            }
        }
        return text;
    }

}
