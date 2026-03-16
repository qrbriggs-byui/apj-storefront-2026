package edu.byui.apj.storefront.web.model;

/**
 * Matches db API response: { "orderId": 123, "status": "PENDING" }.
 * Status is deserialized as String from the db enum (PENDING, PROCESSING, COMPLETED, FAILED).
 */
public record OrderStatusResponse(Long orderId, String status) {}
