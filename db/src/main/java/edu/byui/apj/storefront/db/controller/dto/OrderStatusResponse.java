package edu.byui.apj.storefront.db.controller.dto;

import edu.byui.apj.storefront.db.model.OrderStatus;

/**
 * Response for order creation and status polling. Matches plan: { "orderId": 123, "status": "PENDING" }.
 */
public record OrderStatusResponse(Long orderId, OrderStatus status) {}
