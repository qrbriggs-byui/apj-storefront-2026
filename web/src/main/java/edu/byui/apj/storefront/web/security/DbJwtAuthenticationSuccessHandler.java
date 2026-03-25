package edu.byui.apj.storefront.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Map;

/**
 * After the browser form login succeeds against the web app, calls the db service login to obtain
 * a JWT and stores it in the HTTP session. {@link edu.byui.apj.storefront.web.service.CartClientService}
 * then forwards that token on every cart request (Article 14-2 flow).
 */
@Component
public class DbJwtAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(DbJwtAuthenticationSuccessHandler.class);

    private final WebClient dbServiceClient;

    public DbJwtAuthenticationSuccessHandler(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.dbServiceClient = dbServiceClient;
        setDefaultTargetUrl("/index.html");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (username != null && password != null) {
            try {
                Map<String, Object> body = Map.of("username", username, "password", password);
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenResponse = dbServiceClient.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                if (tokenResponse != null) {
                    Object t = tokenResponse.get("token");
                    if (t instanceof String token) {
                        request.getSession().setAttribute(WebSessionKeys.DB_JWT, token);
                    } else {
                        log.warn("Db login response missing token string for user {}", username);
                    }
                } else {
                    log.warn("Db login response was null for user {}", username);
                }
            } catch (WebClientResponseException e) {
                log.warn("Db login failed: {} {}", e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.warn("Could not obtain JWT from db service", e);
            }
        } else {
            log.warn("Login success but username/password parameters missing; db JWT not stored.");
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
