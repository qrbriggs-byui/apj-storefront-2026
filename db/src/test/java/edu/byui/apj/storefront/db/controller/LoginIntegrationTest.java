package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.messaging.OrderConfirmationProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test (Article 15-2): loads the real Spring application (including Security, JWT
 * service, in-memory users, and H2). We send an HTTP POST like a client would — no mocks for login
 * itself — and assert on status and JSON.
 * <p>
 * {@link OrderConfirmationProducer} is mocked so tests do not require a running Artemis broker
 * (same pattern as {@link edu.byui.apj.storefront.db.DbApplicationTests}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginIntegrationTest {

    /** MockMvc simulates HTTP against your DispatcherServlet without starting a real TCP server. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Replace the JMS producer with a no-op mock so the context loads without Artemis
     * ({@code spring.artemis.broker-url} is not required for this test).
     */
    @MockBean
    @SuppressWarnings("unused")
    private OrderConfirmationProducer orderConfirmationProducer;

    /**
     * End-to-end for login: real {@code AuthenticationManager}, real demo user {@code shopper} /
     * {@code password}, real {@code JwtService} signing — asserts the JSON contract your SPA relies on.
     */
    @Test
    void postLogin_withDemoShopperCredentials_returnsJsonWithNonEmptyToken() throws Exception {
        String body = """
                {"username":"shopper","password":"password"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
