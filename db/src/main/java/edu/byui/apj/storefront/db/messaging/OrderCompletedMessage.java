package edu.byui.apj.storefront.db.messaging;

import java.time.Instant;

/**
 * Lightweight event placed on the JMS queue when an order finishes processing.
 * Full order data is fetched later via REST by the consumer.
 */
public record OrderCompletedMessage(String eventType, Long orderId, Instant completedAt) {

    public static OrderCompletedMessage orderCompleted(Long orderId) {
        return new OrderCompletedMessage("ORDER_COMPLETED", orderId, Instant.now());
    }
}
