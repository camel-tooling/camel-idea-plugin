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
package com.github.cameltooling.idea.completion.header;

import java.util.Objects;

/**
 * Defines a type of endpoints.
 */
final class CamelHeaderEndpoint {

    /**
     * The name of the component corresponding to the endpoint.
     */
    private final String componentName;
    /**
     * Indicates whether the endpoint can only be a consumer.
     */
    private final boolean consumerOnly;
    /**
     * Indicates whether the endpoint can only be a producer.
     */
    private final boolean producerOnly;

    /**
     * Constructs a {@code CamelHeaderEndpoint} with the given parameters.
     * @param componentName the name of the component corresponding to the endpoint
     * @param consumerOnly Indicates whether the endpoint can only be a consumer
     * @param producerOnly Indicates whether the endpoint can only be a producer
     */
    private CamelHeaderEndpoint(String componentName, boolean consumerOnly, boolean producerOnly) {
        this.componentName = componentName;
        this.consumerOnly = consumerOnly;
        this.producerOnly = producerOnly;
    }

    /**
     * @param componentName the name of the component corresponding to the endpoint
     * @return an instance of {@code CamelHeaderEndpoint} representing an endpoint that can only be a consumer.
     */
    static CamelHeaderEndpoint consumerOnly(String componentName) {
        return new CamelHeaderEndpoint(componentName, true, false);
    }

    /**
     * @param componentName the name of the component corresponding to the endpoint
     * @return an instance of {@code CamelHeaderEndpoint} representing an endpoint that can only be a producer.
     */
    static CamelHeaderEndpoint producerOnly(String componentName) {
        return new CamelHeaderEndpoint(componentName, false, true);
    }

    /**
     * @param componentName the name of the component corresponding to the endpoint
     * @return an instance of {@code CamelHeaderEndpoint} representing an endpoint that can be a consumer or a producer.
     */
    static CamelHeaderEndpoint both(String componentName) {
        return new CamelHeaderEndpoint(componentName, false, false);
    }

    /**
     * @return the name of the component corresponding to the endpoint
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * @return {@code true} if the endpoint can only be a consumer, {@code false} otherwise.
     */
    public boolean isConsumerOnly() {
        return consumerOnly;
    }

    /**
     * @return {@code true} if the endpoint can only be a producer, {@code false} otherwise.
     */
    public boolean isProducerOnly() {
        return producerOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelHeaderEndpoint endpoint = (CamelHeaderEndpoint) o;
        return consumerOnly == endpoint.consumerOnly && producerOnly == endpoint.producerOnly && Objects.equals(componentName, endpoint.componentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, consumerOnly, producerOnly);
    }
}
