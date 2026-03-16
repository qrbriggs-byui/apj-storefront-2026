package edu.byui.apj.storefront.db.controller.dto;

public record AddItemRequest(
        String productId,
        String name,
        Double price,
        Integer quantity
) {}