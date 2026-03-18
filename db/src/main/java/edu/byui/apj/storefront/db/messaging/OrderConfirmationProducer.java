package edu.byui.apj.storefront.db.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes an order-completed event to Artemis after the db module marks the order COMPLETED.
 */
@Component
public class OrderConfirmationProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationProducer.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public OrderConfirmationProducer(
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper,
            @Value("${app.jms.order-confirmation-queue}") String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    public void sendOrderCompleted(Long orderId) {
        OrderCompletedMessage payload = OrderCompletedMessage.orderCompleted(orderId);
        try {
            String body = objectMapper.writeValueAsString(payload);
            jmsTemplate.send(queueName, (Session session) -> session.createTextMessage(body));
            log.info("Order {} ORDER_COMPLETED event sent to queue {}", orderId, queueName);
        } catch (Exception e) {
            log.error("Failed to send JMS order completed event for order {}", orderId, e);
        }
    }
}
