package edu.byui.apj.storefront.db.controller.dto;

/** Request body for POST /api/auth/login — validated in UserAccountService. */
public record LoginRequest(String username, String password) {}
