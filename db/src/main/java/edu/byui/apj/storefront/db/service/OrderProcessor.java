package edu.byui.apj.storefront.db.service;

import edu.byui.apj.storefront.db.messaging.OrderConfirmationProducer;
import edu.byui.apj.storefront.db.model.Order;
import edu.byui.apj.storefront.db.model.OrderStatus;
import edu.byui.apj.storefront.db.repository.OrderRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs order completion in a background thread. Must be a separate component so that
 * Spring's proxy invokes @Async when OrderService calls it (self-invocation would not).
 */
@Component
public class OrderProcessor {

    private final OrderRepository orderRepository;
    private final OrderConfirmationProducer orderConfirmationProducer;

    public OrderProcessor(OrderRepository orderRepository, OrderConfirmationProducer orderConfirmationProducer) {
        this.orderRepository = orderRepository;
        this.orderConfirmationProducer = orderConfirmationProducer;
    }

    @Async
    @Transactional
    public void processOrderAsync(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            return;
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        orderConfirmationProducer.sendOrderCompleted(orderId);
    }
}
