package edu.byui.apj.storefront.jms.dto;

import java.time.Instant;

/** Mirrors the JSON payload produced by the db module. */
public record OrderCompletedMessage(String eventType, Long orderId, Instant completedAt) {}
