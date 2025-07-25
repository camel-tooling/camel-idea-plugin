package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.github.cameltooling.idea.util.CamelIdeaUtils.PROPERTY_PLACEHOLDER_PATTERN;
import static com.github.cameltooling.idea.util.CamelIdeaUtils.PROPERTY_PLACEHOLDER_START_TAG;

/**
 * Provides references for Camel property placeholders in the format {{propertyName}}, inside strings in:
 * <ul>
 *     <li>Java code string literals (e.g. in camel route endpoint uris)</li>
 *     <li>XML 'uri' attributes</li>
 * </ul>
 *
 * Using native IDEA {@link PropertyReference} references automatically enables renaming functionality.
 */
public class CamelPropertyPlaceholderReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        List<ElementPattern<? extends PsiElement>> patterns = CamelIdeaUtils.getService().getAllowedPropertyPlaceholderPatterns();
        if (!patterns.isEmpty()) {
            PsiReferenceProvider propertyReferenceProvider = createProvider();
            patterns.forEach(pattern -> {
                registrar.registerReferenceProvider(pattern, propertyReferenceProvider);
            });
        }
    }

    @NotNull
    private PsiReferenceProvider createProvider() {
        return new CamelPsiReferenceProvider() {
            @Override
            public PsiReference[] getCamelReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                String text = element.getText();

                List<PsiReference> references = new ArrayList<>();
                Matcher matcher = PROPERTY_PLACEHOLDER_PATTERN.matcher(text);

                while (matcher.find()) {
                    PropertyReference propertyReference = getPropertyReference(element, matcher);
                    references.add(propertyReference);
                }

                return references.toArray(new PsiReference[0]);
            }

            private static @NotNull PropertyReference getPropertyReference(@NotNull PsiElement element, Matcher matcher) {
                String propertyName = matcher.group(1);
                int startOffset = matcher.start() + PROPERTY_PLACEHOLDER_START_TAG.length();
                int endOffset = startOffset + propertyName.length();

                TextRange textRange = new TextRange(startOffset, endOffset);
                return new PropertyReference(
                        propertyName,
                        element,
                        null,
                        true,
                        textRange
                );
            }
        };
    }

}