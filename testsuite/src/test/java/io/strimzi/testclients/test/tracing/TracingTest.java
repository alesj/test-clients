/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.testclients.test.tracing;

import io.strimzi.test.container.StrimziKafkaContainer;
import io.strimzi.testclients.http.consumer.HttpConsumerApp;
import io.strimzi.testclients.http.consumer.HttpConsumerConfiguration;
import io.strimzi.testclients.http.producer.HttpProducerApp;
import io.strimzi.testclients.http.producer.HttpProducerConfiguration;
import io.strimzi.testclients.test.support.JaegerContainer;
import io.strimzi.testclients.test.support.KafkaBridgeContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startable;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class TracingTest {
    private static final Logger log = LoggerFactory.getLogger(TracingTest.class);

    private static final String TOPIC = "mytracing";

    @Container
    JoinedContainer container = new JoinedContainer();

    @Test
    public void testTracing() throws Exception {
        HttpProducerConfiguration producerConfig = new HttpProducerConfiguration(
            container.getHost(), container.getPort(), TOPIC
        );
        HttpProducerApp.start(producerConfig, false);

        HttpConsumerConfiguration consumerConfig = new HttpConsumerConfiguration(
            container.getHost(), container.getPort(), TOPIC
        );
        HttpConsumerApp.start(consumerConfig, false);

        // TODO check for traces
    }

    // Order matters
    private static class JoinedContainer implements Startable {
        StrimziKafkaContainer kafkaContainer = new StrimziKafkaContainer();
        JaegerContainer jaegerContainer;
        KafkaBridgeContainer bridgeContainer;

        String getHost() {
            return bridgeContainer.getHost();
        }

        String getPort() {
            return String.valueOf(bridgeContainer.getEmbeddedHttpServerPort());
        }

        @Override
        public void start() {
            String tracing = System.getProperty("tracing");
            if (tracing == null) {
                throw new IllegalStateException("Missing -Dtracing system property");
            }

            kafkaContainer.start();

            jaegerContainer = new JaegerContainer();
            jaegerContainer.start();

            Map<String, String> envs = new HashMap<>();
            if ("opentracing".equals(tracing)) {
                envs.put("JAEGER_SERVICE_NAME", "mybridge");
            } else if ("opentelemetry".equals(tracing)) {
                envs.put("OTEL_SERVICE_NAME", "mybridge");
                envs.put(
                    "OTEL_EXPORTER_JAEGER_ENDPOINT",
                    String.format("http://%s:%s", jaegerContainer.getHost(), jaegerContainer.getMappedPort(JaegerContainer.JAEGER_COLLECTOR_GRPC_PORT))
                );
            } else {
                throw new IllegalArgumentException("Invalid -Dtracing value: " + tracing);
            }

            log.info("Envs: {}", envs);

            bridgeContainer = new KafkaBridgeContainer(() -> kafkaContainer.getBootstrapServers()).withEnv(envs);
            bridgeContainer.start();
        }

        @Override
        public void stop() {
            bridgeContainer.stop();
            jaegerContainer.stop();
            kafkaContainer.stop();
        }
    }
}
