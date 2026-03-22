package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.LoginApiRequest;
import edu.byui.apj.storefront.web.model.RegisterApiRequest;
import edu.byui.apj.storefront.web.model.UserAccountResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

/**
 * Proxies account operations to the db module REST API (same pattern as CartClientService).
 */
@Service
public class AccountClientService {

    private final WebClient webClient;

    public AccountClientService(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.webClient = dbServiceClient;
    }

    public Optional<UserAccountResponse> register(RegisterApiRequest request) {
        try {
            UserAccountResponse body = webClient.post()
                    .uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block();
            return Optional.ofNullable(body);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<UserAccountResponse> login(LoginApiRequest request) {
        try {
            UserAccountResponse body = webClient.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block();
            return Optional.ofNullable(body);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<UserAccountResponse> getProfile(Long userId) {
        try {
            UserAccountResponse body = webClient.get()
                    .uri("/api/users/{id}/profile", userId)
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block();
            return Optional.ofNullable(body);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
