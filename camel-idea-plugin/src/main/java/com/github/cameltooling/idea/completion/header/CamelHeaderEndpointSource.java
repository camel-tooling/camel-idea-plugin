package com.github.cameltooling.idea.completion.header;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public enum CamelHeaderEndpointSource {
    PRODUCER_ONLY {
        @Override
        Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element) {
            return new ProducerEndpointFinder(element).findEndpoints();
        }
    },

    CONSUMER_ONLY {
        @Override
        Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element) {
            return new ConsumerEndpointFinder(element).findEndpoints();
        }
    },

    ALL {
        @Override
        Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element) {
            final Collection<CamelHeaderEndpoint> consumerEndpoints = CONSUMER_ONLY.getEndpoints(element);
            final Collection<CamelHeaderEndpoint> producerEndpoints = PRODUCER_ONLY.getEndpoints(element);

            return Stream.concat(consumerEndpoints.stream(), producerEndpoints.stream())
                    .map(CamelHeaderEndpoint::getComponentName)
                    .map(CamelHeaderEndpoint::both)
                    .collect(Collectors.toSet());
        }
    };

    abstract Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element);

    private static abstract class EndpointFinder {
        final PsiElement element;

        EndpointFinder(PsiElement element) {
            this.element = element;
        }

        Collection<CamelHeaderEndpoint> findEndpoints() {
            final CamelIdeaUtils utils = CamelIdeaUtils.getService();
            final Module module = ModuleUtilCore.findModuleForPsiElement(element);

            return utils.findEndpointUsages(module, e -> e.indexOf(':') != -1)
                    .stream()
                    .filter(this::matchesEndpointType)
                    .map(PsiElement::getText)
                    .map(StringUtil::unquoteString)
                    .map(StringUtils::asComponentName)
                    .map(this::createEndpoint)
                    .collect(Collectors.toSet());
        }

        abstract boolean matchesEndpointType(PsiElement element);

        CamelHeaderEndpoint createEndpoint(String componentName) {
            return CamelHeaderEndpoint.producerOnly(componentName);
        }
    }

    private static class ProducerEndpointFinder extends EndpointFinder {
        ProducerEndpointFinder(PsiElement element) {
            super(element);
        }

        @Override
        boolean matchesEndpointType(PsiElement element) {
            return CamelIdeaUtils.getService().isProducerEndpoint(element);
        }
    }

    private static class ConsumerEndpointFinder extends EndpointFinder {
        ConsumerEndpointFinder(PsiElement element) {
            super(element);
        }

        @Override
        boolean matchesEndpointType(PsiElement element) {
            return CamelIdeaUtils.getService().isConsumerEndpoint(element);
        }

        @Override
        CamelHeaderEndpoint createEndpoint(String componentName) {
            return CamelHeaderEndpoint.consumerOnly(componentName);
        }
    }
}
