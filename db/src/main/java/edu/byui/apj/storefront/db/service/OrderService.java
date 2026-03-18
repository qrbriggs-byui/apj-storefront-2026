package edu.byui.apj.storefront.db.service;

import edu.byui.apj.storefront.db.controller.dto.CreateOrderRequest;
import edu.byui.apj.storefront.db.controller.dto.OrderDetailsItemResponse;
import edu.byui.apj.storefront.db.controller.dto.OrderDetailsResponse;
import edu.byui.apj.storefront.db.model.*;
import edu.byui.apj.storefront.db.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderProcessor orderProcessor;

    public OrderService(OrderRepository orderRepository,
                       CartService cartService,
                       OrderProcessor orderProcessor) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.orderProcessor = orderProcessor;
    }

    /**
     * Creates an order from the current cart contents and customer/shipping info, saves as PENDING,
     * starts async processing, and returns immediately so the client can redirect and poll for status.
     */
    @Transactional
    public Order createOrderFromCart(CreateOrderRequest request) {
        Cart cart = cartService.getCart(request.cartId());
        Order order = new Order();
        order.setCartId(request.cartId());
        order.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setShippingAddressLine1(request.shippingAddressLine1());
        order.setShippingAddressLine2(request.shippingAddressLine2());
        order.setShippingCity(request.shippingCity());
        order.setShippingState(request.shippingState());
        order.setShippingPostalCode(request.shippingPostalCode());
        order.setShippingCountry(request.shippingCountry());
        double total = 0;
        for (Item cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setProductId(cartItem.getProductId());
            oi.setProductName(cartItem.getName());
            oi.setPrice(cartItem.getPrice());
            oi.setQuantity(cartItem.getQuantity());
            oi.setOrder(order);
            order.getItems().add(oi);
            total += cartItem.getPrice() * cartItem.getQuantity();
        }
        order.setTotalAmount(total);
        order = orderRepository.saveAndFlush(order);
        orderProcessor.processOrderAsync(order.getId());
        return order;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Loads order with line items for the JMS consumer (confirmation workflow).
     */
    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.getItems().size();
        List<OrderDetailsItemResponse> items = order.getItems().stream()
                .map(oi -> new OrderDetailsItemResponse(
                        oi.getProductId(),
                        oi.getProductName(),
                        oi.getQuantity(),
                        oi.getPrice()))
                .toList();
        return new OrderDetailsResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCartId(),
                items);
    }
}
