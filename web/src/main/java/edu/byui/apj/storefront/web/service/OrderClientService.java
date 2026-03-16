package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.OrderStatusResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Calls the db module order REST API via WebClient. Used by CheckoutController for
 * creating orders and polling order status.
 */
@Service
public class OrderClientService {

    private final WebClient webClient;

    public OrderClientService(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.webClient = dbServiceClient;
    }

    /**
     * POST /api/orders with cartId and customer/shipping info. Db creates order, starts async processing, returns immediately.
     */
    public Optional<OrderStatusResponse> createOrder(Long cartId,
                                                     String customerName,
                                                     String customerEmail,
                                                     String shippingAddressLine1,
                                                     String shippingAddressLine2,
                                                     String shippingCity,
                                                     String shippingState,
                                                     String shippingPostalCode,
                                                     String shippingCountry) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("cartId", cartId);
            body.put("customerName", customerName);
            body.put("customerEmail", customerEmail);
            body.put("shippingAddressLine1", shippingAddressLine1);
            body.put("shippingAddressLine2", shippingAddressLine2);
            body.put("shippingCity", shippingCity);
            body.put("shippingState", shippingState);
            body.put("shippingPostalCode", shippingPostalCode);
            body.put("shippingCountry", shippingCountry);
            OrderStatusResponse response = webClient.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(OrderStatusResponse.class)
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * GET /api/orders/{orderId}. Used by the confirmation page to poll for status (e.g. COMPLETED).
     */
    public Optional<OrderStatusResponse> getOrderStatus(Long orderId) {
        try {
            OrderStatusResponse response = webClient.get()
                    .uri("/api/orders/{orderId}", orderId)
                    .retrieve()
                    .bodyToMono(OrderStatusResponse.class)
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
