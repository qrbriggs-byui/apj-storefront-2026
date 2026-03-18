package edu.byui.apj.storefront.jms.dto;

public record OrderDetailsItemDto(String productId, String productName, int quantity, double price) {}
