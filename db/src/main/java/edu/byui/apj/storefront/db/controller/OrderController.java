package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.controller.dto.CreateOrderRequest;
import edu.byui.apj.storefront.db.controller.dto.OrderDetailsResponse;
import edu.byui.apj.storefront.db.controller.dto.OrderStatusResponse;
import edu.byui.apj.storefront.db.model.Order;
import edu.byui.apj.storefront.db.model.OrderStatus;
import edu.byui.apj.storefront.db.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderStatusResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrderFromCart(request);
        return ResponseEntity.ok(new OrderStatusResponse(order.getId(), order.getStatus()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(new OrderStatusResponse(order.getId(), order.getStatus()));
    }

    @GetMapping("/{orderId}/details")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetails(orderId));
    }
}
