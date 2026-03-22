package edu.byui.apj.storefront.db.controller.dto;

/**
 * Request body for POST /api/auth/register — validated in {@link edu.byui.apj.storefront.db.service.UserAccountService}.
 */
public record RegisterRequest(
        String username,
        String password,
        String firstName,
        String lastName,
        String shippingZip
) {}
