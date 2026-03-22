package edu.byui.apj.storefront.web.model;

/** JSON body sent to the db module for login. */
public record LoginApiRequest(String username, String password) {}
