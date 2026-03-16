package edu.byui.apj.storefront.web.model;

import java.time.Instant;
import java.util.List;

/**
 * Web-layer DTO for the cart. Matches the structure returned by the db cart API.
 */
public record CartDTO(
        Long id,
        Instant createdAt,
        List<CartItemDTO> items
) {}
