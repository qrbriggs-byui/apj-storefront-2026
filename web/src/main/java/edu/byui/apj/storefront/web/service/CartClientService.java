package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.CartDTO;
import edu.byui.apj.storefront.web.model.CartItemDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Calls the db module cart REST API via WebClient. Converts responses to web DTOs.
 */
@Service
public class CartClientService {

    private final WebClient webClient;

    public CartClientService(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.webClient = dbServiceClient;
    }

    public Optional<CartDTO> getCart(Long cartId) {
        return webClient.get()
                .uri("/api/cart/{cartId}", cartId)
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.error(new RuntimeException("Cart API error: " + response.statusCode())))
                .bodyToMono(CartDTO.class)
                .map(Optional::of)
                .onErrorReturn(Optional.empty())
                .block();
    }

    public Optional<Long> createCart() {
        CartDTO created = webClient.post()
                .uri("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(CartDTO.class)
                .block();
        return created != null ? Optional.of(created.id()) : Optional.empty();
    }

    public Optional<CartItemDTO> addItem(Long cartId, String productId, String productName, double price, int quantity) {
        Map<String, Object> body = Map.of(
                "productId", productId != null ? productId : "",
                "name", productName != null ? productName : "",
                "price", price,
                "quantity", quantity
        );
        CartItemDTO item = webClient.post()
                .uri("/api/cart/{cartId}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CartItemDTO.class)
                .block();
        return Optional.ofNullable(item);
    }

    public boolean updateItem(Long cartId, Long itemId, int quantity) {
        try {
            webClient.put()
                    .uri("/api/cart/{cartId}/items/{itemId}", cartId, itemId)
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

    public boolean removeItem(Long cartId, Long itemId) {
        try {
            webClient.delete()
                    .uri("/api/cart/{cartId}/items/{itemId}", cartId, itemId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clearCart(Long cartId) {
        try {
            webClient.delete()
                    .uri("/api/cart/{cartId}", cartId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
