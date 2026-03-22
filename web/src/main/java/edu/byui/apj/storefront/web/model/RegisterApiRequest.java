package edu.byui.apj.storefront.web.model;

/**
 * JSON body sent to the db module for registration (same field names as db RegisterRequest).
 */
public record RegisterApiRequest(
        String username,
        String password,
        String firstName,
        String lastName,
        String shippingZip
) {}
