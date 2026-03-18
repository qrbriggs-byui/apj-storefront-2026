package edu.byui.apj.storefront.db.controller.dto;

import java.time.Instant;
import java.util.List;

public record OrderDetailsResponse(
        Long orderId,
        Instant createdAt,
        String status,
        double totalAmount,
        Long cartId,
        List<OrderDetailsItemResponse> items
) {}
