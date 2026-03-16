package edu.byui.apj.storefront.db.controller.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for the cart resource. Exposed by the cart API to avoid leaking JPA entities.
 */
public record CartDTO(
        Long id,
        Instant createdAt,
        List<CartItemDTO> items
) {}
