package edu.byui.apj.storefront.db.controller.dto;

/**
 * Safe view of a user account for REST responses — never includes password hash.
 */
public record UserAccountResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String shippingZip
) {}
