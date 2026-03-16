package edu.byui.apj.storefront.db.model;

/**
 * Lifecycle status of an order. Used for async processing and polling.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
