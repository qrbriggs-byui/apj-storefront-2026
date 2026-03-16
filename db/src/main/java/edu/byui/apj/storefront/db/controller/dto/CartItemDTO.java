package edu.byui.apj.storefront.db.controller.dto;

/**
 * DTO for a single cart line item. Exposed by the cart API to avoid leaking JPA entities.
 */
public record CartItemDTO(
        Long id,
        String productId,
        String productName,
        int quantity,
        double price
) {}
