package edu.byui.apj.storefront.jms.dto;

import java.time.Instant;
import java.util.List;

public record OrderDetailsDto(
        Long orderId,
        Instant createdAt,
        String status,
        double totalAmount,
        Long cartId,
        List<OrderDetailsItemDto> items
) {}
