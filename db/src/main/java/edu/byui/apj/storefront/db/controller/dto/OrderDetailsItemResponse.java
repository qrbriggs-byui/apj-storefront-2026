package edu.byui.apj.storefront.db.controller.dto;

public record OrderDetailsItemResponse(
        String productId,
        String productName,
        int quantity,
        double price
) {}
