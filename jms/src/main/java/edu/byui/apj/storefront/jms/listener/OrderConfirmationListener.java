package edu.byui.apj.storefront.jms.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.byui.apj.storefront.jms.client.OrderDetailsClient;
import edu.byui.apj.storefront.jms.dto.OrderCompletedMessage;
import edu.byui.apj.storefront.jms.dto.OrderDetailsDto;
import edu.byui.apj.storefront.jms.service.OrderConfirmationMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationListener.class);

    private final ObjectMapper objectMapper;
    private final OrderDetailsClient orderDetailsClient;
    private final OrderConfirmationMessageService messageService;

    public OrderConfirmationListener(
            ObjectMapper objectMapper,
            OrderDetailsClient orderDetailsClient,
            OrderConfirmationMessageService messageService) {
        this.objectMapper = objectMapper;
        this.orderDetailsClient = orderDetailsClient;
        this.messageService = messageService;
    }

    @JmsListener(destination = "${app.jms.order-confirmation-queue}")
    public void onOrderCompleted(String jsonPayload) {
        try {
            OrderCompletedMessage event = objectMapper.readValue(jsonPayload, OrderCompletedMessage.class);
            if (event.orderId() == null) {
                log.warn("Ignoring JMS message with null orderId: {}", jsonPayload);
                return;
            }
            log.info("Received JMS message for completed order {}", event.orderId());
            log.info("Fetching order details from db module for order {}", event.orderId());
            OrderDetailsDto details = orderDetailsClient.getOrderDetails(event.orderId());
            String confirmation = messageService.buildConfirmationMessage(details);
            log.info("Generated confirmation message for order {}", event.orderId());
            System.out.println(confirmation);
        } catch (Exception e) {
            log.error("Failed to process order confirmation message", e);
            throw new RuntimeException(e);
        }
    }
}
