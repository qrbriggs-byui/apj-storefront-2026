package edu.byui.apj.storefront.jms.client;

import edu.byui.apj.storefront.jms.dto.OrderDetailsDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OrderDetailsClient {

    private final WebClient dbWebClient;

    public OrderDetailsClient(WebClient dbServiceWebClient) {
        this.dbWebClient = dbServiceWebClient;
    }

    public OrderDetailsDto getOrderDetails(Long orderId) {
        return dbWebClient.get()
                .uri("/api/orders/{orderId}/details", orderId)
                .retrieve()
                .bodyToMono(OrderDetailsDto.class)
                .block();
    }
}
