package edu.byui.apj.storefront.web.model;

/**
 * Mirrors the db module JSON for account/profile — used when deserializing WebClient responses.
 */
public record UserAccountResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String shippingZip
) {}
