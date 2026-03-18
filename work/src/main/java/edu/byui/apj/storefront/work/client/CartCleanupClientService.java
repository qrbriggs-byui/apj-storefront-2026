package edu.byui.apj.storefront.work.client;

import edu.byui.apj.storefront.work.dto.CartCleanupResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CartCleanupClientService {

    private final WebClient webClient;

    public CartCleanupClientService(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.webClient = dbServiceClient;
    }

    public CartCleanupResponse triggerCleanup(int maxAgeMinutes) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/internal/cart-cleanup")
                        .queryParam("maxAgeMinutes", maxAgeMinutes)
                        .build())
                .retrieve()
                .bodyToMono(CartCleanupResponse.class)
                .block();
    }
}
