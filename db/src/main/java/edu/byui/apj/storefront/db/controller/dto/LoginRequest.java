package edu.byui.apj.storefront.db.controller.dto;

/**
 * JSON body for {@code POST /api/auth/login}.
 */
public record LoginRequest(String username, String password) {
}
