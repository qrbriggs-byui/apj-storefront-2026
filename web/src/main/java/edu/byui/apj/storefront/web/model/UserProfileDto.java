package edu.byui.apj.storefront.web.model;

/**
 * JSON returned by {@code GET /api/me/profile} (read-only name and zip).
 */
public record UserProfileDto(String name, String zipCode) {
}
