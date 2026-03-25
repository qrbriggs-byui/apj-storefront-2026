package edu.byui.apj.storefront.db.controller.dto;

/**
 * JSON returned after successful login (Article 14-2 style).
 */
public record LoginResponse(String token) {
}
