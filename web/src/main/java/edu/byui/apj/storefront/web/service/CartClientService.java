package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.CartDTO;
import edu.byui.apj.storefront.web.model.CartItemDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Calls the db module cart REST API via WebClient. Sends the JWT from the browser session on every
 * request so the db service can validate {@code Authorization: Bearer} (Article 14-2).
 */
@Service
public class CartClientService {

    private final WebClient webClient;

    public CartClientService(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.webClient = dbServiceClient;
    }

    public Optional<CartDTO> getCart(Long cartId, String bearerToken) {
        return webClient.get()
                .uri("/api/cart/{cartId}", cartId)
                .headers(h -> h.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.error(new RuntimeException("Cart API error: " + response.statusCode())))
                .bodyToMono(CartDTO.class)
                .map(Optional::of)
                .onErrorReturn(Optional.empty())
                .block();
    }

    public Optional<Long> createCart(String bearerToken) {
        CartDTO created = webClient.post()
                .uri("/api/cart")
                .headers(h -> h.setBearerAuth(bearerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(CartDTO.class)
                .block();
        return created != null ? Optional.of(created.id()) : Optional.empty();
    }

    public Optional<CartItemDTO> addItem(Long cartId, String bearerToken, String productId, String productName, double price, int quantity) {
        Map<String, Object> body = Map.of(
                "productId", productId != null ? productId : "",
                "name", productName != null ? productName : "",
                "price", price,
                "quantity", quantity
        );
        CartItemDTO item = webClient.post()
                .uri("/api/cart/{cartId}/items", cartId)
                .headers(h -> h.setBearerAuth(bearerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CartItemDTO.class)
                .block();
        return Optional.ofNullable(item);
    }

    public boolean updateItem(Long cartId, Long itemId, int quantity, String bearerToken) {
        try {
            webClient.put()
                    .uri("/api/cart/{cartId}/items/{itemId}", cartId, itemId)
                    .headers(h -> h.setBearerAuth(bearerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeItem(Long cartId, Long itemId, String bearerToken) {
        try {
            webClient.delete()
                    .uri("/api/cart/{cartId}/items/{itemId}", cartId, itemId)
                    .headers(h -> h.setBearerAuth(bearerToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clearCart(Long cartId, String bearerToken) {
        try {
            webClient.delete()
                    .uri("/api/cart/{cartId}", cartId)
                    .headers(h -> h.setBearerAuth(bearerToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
