package edu.byui.apj.storefront.web.model;

/**
 * Returned by {@code GET /api/me/session} for static JavaScript (header cart visibility, product page).
 */
public record SessionResponse(boolean authenticated) {
}
