package edu.byui.apj.storefront.web.security;

/**
 * HttpSession attribute keys shared between login handling and cart calls to the db service.
 */
public final class WebSessionKeys {

    /** JWT returned by the db module {@code POST /api/auth/login}; sent as {@code Authorization: Bearer}. */
    public static final String DB_JWT = "dbJwt";

    private WebSessionKeys() {
    }
}
