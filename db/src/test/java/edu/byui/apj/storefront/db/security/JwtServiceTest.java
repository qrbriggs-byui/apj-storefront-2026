package edu.byui.apj.storefront.db.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}: no Spring context, no database, no mocks — we exercise the real
 * signing and parsing logic in isolation (see Article 15-1: “unit test a small piece of code”).
 */
class JwtServiceTest {

    /** HS256 requires a secret of at least 256 bits (32 bytes); same rule as production {@link JwtService}. */
    private static final String TEST_SECRET = "unit-test-jwt-secret-must-be-at-least-32-bytes!!";

    private JwtService jwtService;

    /** Construct a fresh service before each test so tests stay independent. */
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
    }

    /**
     * Happy path: mint a JWT and parse it the same way {@code JwtAuthenticationFilter} does on each
     * request — subject (username) must round-trip unchanged.
     */
    @Test
    void generateToken_then_parseAndValidate_returnsSameSubject() {
        // Given: a username the issuer puts in the token
        String username = "shopper";

        // When: sign then validate (two public methods your production code uses)
        String token = jwtService.generateToken(username);
        var claims = jwtService.parseAndValidate(token);

        // Then: JWT "sub" claim matches — this is how stateless identity is carried
        assertThat(claims.getSubject()).isEqualTo(username);
    }

    /** Sanity check: tokens should not already be expired when issued. */
    @Test
    void generateToken_setsFutureExpiration() {
        String token = jwtService.generateToken("manager");
        var claims = jwtService.parseAndValidate(token);

        assertThat(claims.getExpiration()).isAfter(new Date());
    }
}
