package edu.byui.apj.storefront.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test (Article 15-2): full web {@link org.springframework.context.ApplicationContext},
 * real {@link org.springframework.security.web.SecurityFilterChain}, and MockMvc driving HTTP.
 * <p>
 * {@code GET /api/me/session} is permitted for anonymous callers; we assert JSON for both anonymous
 * and authenticated scenarios using {@link WithMockUser}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Unauthenticated request: SecurityContext has no logged-in user → JSON says authenticated false.
     * Your static JS uses this to hide cart links until login.
     */
    @Test
    void getSession_whenAnonymous_returnsAuthenticatedFalse() throws Exception {
        mockMvc.perform(get("/api/me/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    /**
     * {@link WithMockUser} installs a fake authenticated user into the test context for this method
     * only — simulates a logged-in session without browser cookies.
     */
    @Test
    @WithMockUser(username = "shopper", roles = "USER")
    void getSession_whenLoggedIn_returnsAuthenticatedTrue() throws Exception {
        mockMvc.perform(get("/api/me/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }
}
