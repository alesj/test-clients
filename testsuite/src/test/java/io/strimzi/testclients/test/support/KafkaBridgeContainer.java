/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.testclients.test.support;

import org.testcontainers.containers.GenericContainer;

public class KafkaBridgeContainer extends GenericContainer<KafkaBridgeContainer> {
    public KafkaBridgeContainer() {
        super(ContainerUtil.getImageName(
            "KAFKA_BRIDGE_IMAGE",
            "quay.io/strimzi/kafka-bridge:0.21.4")
        );
    }
}
