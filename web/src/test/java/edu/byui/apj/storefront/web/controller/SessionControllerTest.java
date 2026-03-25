package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.SessionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link SessionController} (Article 15-1): no Spring context, no MockMvc — we
 * call the controller method with hand-built {@link org.springframework.security.core.Authentication}
 * objects to match how the security filter populates the context.
 */
class SessionControllerTest {

    private final SessionController controller = new SessionController();

    @Test
    void session_whenAuthenticationNull_reportsUnauthenticated() {
        // No security context yet: static pages treat as logged out
        SessionResponse response = controller.session(null);

        assertThat(response.authenticated()).isFalse();
    }

    @Test
    void session_whenAnonymousToken_reportsUnauthenticated() {
        // AnonymousAuthenticationToken is how Spring represents "anonymous" — still a kind of
        // Authentication, but SessionController explicitly excludes it from "logged in"
        var anonymous = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        SessionResponse response = controller.session(anonymous);

        assertThat(response.authenticated()).isFalse();
    }

    @Test
    void session_whenNonAnonymousAuthenticatedUser_reportsAuthenticated() {
        // Three-arg UsernamePasswordAuthenticationToken = authenticated principal with roles
        // (do not call setAuthenticated(true) on the two-arg constructor — Spring Security 6 forbids it)
        var auth = new UsernamePasswordAuthenticationToken(
                "shopper",
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SessionResponse response = controller.session(auth);

        assertThat(response.authenticated()).isTrue();
    }
}
