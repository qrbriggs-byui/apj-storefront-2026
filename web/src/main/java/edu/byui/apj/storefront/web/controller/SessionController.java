package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.SessionResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anonymous-safe JSON so static pages can show or hide cart UI without calling protected profile APIs.
 */
@RestController
@RequestMapping("/api/me")
public class SessionController {

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return new SessionResponse(authenticated);
    }
}
