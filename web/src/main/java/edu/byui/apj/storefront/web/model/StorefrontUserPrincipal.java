package edu.byui.apj.storefront.web.model;

/**
 * Custom principal stored in the Spring Security context after login or registration.
 * Holds the database user id so the web tier can call GET /api/users/{id}/profile on the db module.
 */
public record StorefrontUserPrincipal(Long id, String username) {}
