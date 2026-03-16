package edu.byui.apj.storefront.web.model;

/**
 * Web-layer DTO for a cart line item. Matches the structure returned by the db cart API.
 */
public record CartItemDTO(
        Long id,
        String productId,
        String productName,
        int quantity,
        double price
) {}
