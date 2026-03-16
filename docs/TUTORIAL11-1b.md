# Tutorial 11-1b: Asynchronous Checkout and Order Processing

This tutorial adds **checkout and order confirmation** with **asynchronous order processing** so that placing an order returns immediately while payment is simulated in the background. Students copy/paste the Java code and properties below; the instructor will provide the up-to-date static folder (checkout page, order-confirmation page, and polling JavaScript) in the last step.

**Architecture:** Browser → web module → db module. The db module creates orders and runs a long-running “payment” step with Spring’s **@Async** so the HTTP response is not blocked. The UI polls for order status until the order is COMPLETED.

**Reference:** `docs/ASYNC_ORDER.md`

---

## Part 1: OrderStatus enum (db module)

Create an enum for order lifecycle. The API and polling will return this status.

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/model/OrderStatus.java`

```java
package edu.byui.apj.storefront.db.model;

/**
 * Lifecycle status of an order. Used for async processing and polling.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

**What it does:** Orders start as PENDING, move to PROCESSING when the async job runs, then to COMPLETED (or FAILED on error). The confirmation page polls until status is COMPLETED.

---

## Part 2: OrderItem entity (db module)

Represents one line item **at checkout**—a snapshot of product id, name, price, and quantity. It is not tied to the cart; it is copied from the cart when the order is created.

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/model/OrderItem.java`

```java
package edu.byui.apj.storefront.db.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Snapshot of one line item at checkout. Not linked to cart Item; copied from cart at order creation.
 */
@Entity
@Table(name = "order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue
    private Long id;

    private String productId;
    private String productName;
    private double price;
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
```

---

## Part 3: Order entity (db module)

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/model/Order.java`

```java
package edu.byui.apj.storefront.db.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private Long cartId;
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private double totalAmount;

    /* Customer and shipping (stored with order at checkout) */
    private String customerName;
    private String customerEmail;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
```

**What it does:** Stores cart id, creation time, status, total, customer and shipping info captured at checkout, and the list of order items. The table name `orders` avoids conflict with the SQL keyword.

---

## Part 4: Order and OrderItem repositories (db module)

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/repository/OrderRepository.java`

```java
package edu.byui.apj.storefront.db.repository;

import edu.byui.apj.storefront.db.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/repository/OrderItemRepository.java`

```java
package edu.byui.apj.storefront.db.repository;

import edu.byui.apj.storefront.db.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
```

---

## Part 5: Enable @Async (db module)

Spring must be told to run `@Async` methods in a separate thread pool so that the HTTP request can return immediately while work continues in the background.

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/config/AsyncConfig.java`

```java
package edu.byui.apj.storefront.db.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async method execution. Async methods run in a separate thread pool
 * so the calling thread returns immediately (e.g. HTTP response) while work continues in the background.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
```

---

## Part 6: OrderProcessor (db module) — @Async payment simulation

The “slow payment” logic runs in a **separate component** so that Spring’s proxy applies `@Async`. If the same class called its own `@Async` method, it would not run asynchronously.

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/service/OrderProcessor.java`

```java
package edu.byui.apj.storefront.db.service;

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

    public OrderProcessor(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Async
    @Transactional
    public void processOrderAsync(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            return;
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }
}
```

**What it does:** Updates the order to PROCESSING, sleeps 10 seconds (simulated payment), then sets COMPLETED. Because of `@Async`, the HTTP request that triggered order creation returns right away; this runs on another thread.

---

## Part 7: OrderService (db module)

Creates an order from the current cart and customer/shipping info, and delegates the long-running work to `OrderProcessor`.

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/service/OrderService.java`

```java
package edu.byui.apj.storefront.db.service;

import edu.byui.apj.storefront.db.controller.dto.CreateOrderRequest;
import edu.byui.apj.storefront.db.model.*;
import edu.byui.apj.storefront.db.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
}
```

**What it does:** Loads the cart, builds an `Order` with customer and shipping fields from the request, adds `OrderItem`s from the cart, saves the order, then calls `OrderProcessor.processOrderAsync(orderId)` so the 10-second simulation runs in the background. The method returns the saved order so the controller can respond with `orderId` and status immediately.

---

## Part 8: Order API DTOs (db module)

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/controller/dto/CreateOrderRequest.java`

```java
package edu.byui.apj.storefront.db.controller.dto;

/**
 * Request body for POST /api/orders. Cart to convert into an order plus customer and shipping info.
 */
public record CreateOrderRequest(
        Long cartId,
        String customerName,
        String customerEmail,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,
        String shippingState,
        String shippingPostalCode,
        String shippingCountry
) {
    /** Convenience: all customer/shipping fields optional (null). */
    public static CreateOrderRequest withDefaults(Long cartId) {
        return new CreateOrderRequest(cartId, null, null, null, null, null, null, null, null);
    }
}
```

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/controller/dto/OrderStatusResponse.java`

```java
package edu.byui.apj.storefront.db.controller.dto;

import edu.byui.apj.storefront.db.model.OrderStatus;

/**
 * Response for order creation and status polling. Matches plan: { "orderId": 123, "status": "PENDING" }.
 */
public record OrderStatusResponse(Long orderId, OrderStatus status) {}
```

---

## Part 9: OrderController (db module)

**Path:** `db/src/main/java/edu/byui/apj/storefront/db/controller/OrderController.java`

```java
package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.controller.dto.CreateOrderRequest;
import edu.byui.apj.storefront.db.controller.dto.OrderStatusResponse;
import edu.byui.apj.storefront.db.model.Order;
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
}
```

**What it does:** POST creates an order from the cart and returns `orderId` and status (PENDING) right away. GET returns the current status for polling. The db module does not need any new properties; it already has a base URL that the web module uses.

---

## Part 10: OrderStatusResponse (web module)

The web module needs a DTO that matches the JSON from the db API. The db sends `status` as the enum name (e.g. `"PENDING"`); using `String` in the web DTO avoids depending on the db’s enum.

**Path:** `web/src/main/java/edu/byui/apj/storefront/web/model/OrderStatusResponse.java`

```java
package edu.byui.apj.storefront.web.model;

/**
 * Matches db API response: { "orderId": 123, "status": "PENDING" }.
 * Status is deserialized as String from the db enum (PENDING, PROCESSING, COMPLETED, FAILED).
 */
public record OrderStatusResponse(Long orderId, String status) {}
```

---

## Part 11: OrderClientService (web module)

**Path:** `web/src/main/java/edu/byui/apj/storefront/web/service/OrderClientService.java`

```java
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
```

**What it does:** Forwards order creation (with customer and shipping data) and status checks to the db module. Uses the same `dbServiceClient` WebClient as the cart API. No new properties are required if `db.service.base-url` is already set.

---

## Part 12: CheckoutController (web module)

**Path:** `web/src/main/java/edu/byui/apj/storefront/web/controller/CheckoutController.java`

```java
package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.OrderStatusResponse;
import edu.byui.apj.storefront.web.service.CartClientService;
import edu.byui.apj.storefront.web.service.OrderClientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Handles checkout page, order creation (POST /checkout), order confirmation page,
 * and order status polling. Uses session cartId; after placing order, creates a new empty cart.
 */
@Controller
public class CheckoutController {

    private static final String SESSION_CART_ID = "cartId";

    private final OrderClientService orderClientService;
    private final CartClientService cartClientService;

    public CheckoutController(OrderClientService orderClientService, CartClientService cartClientService) {
        this.orderClientService = orderClientService;
        this.cartClientService = cartClientService;
    }

    @GetMapping("/checkout")
    public String checkoutPage() {
        return "redirect:/checkout.html";
    }

    @PostMapping("/checkout")
    public String placeOrder(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String shippingAddressLine1,
            @RequestParam(required = false) String shippingAddressLine2,
            @RequestParam(required = false) String shippingCity,
            @RequestParam(required = false) String shippingState,
            @RequestParam(required = false) String shippingPostalCode,
            @RequestParam(required = false) String shippingCountry,
            HttpSession session) {
        Long cartId = (Long) session.getAttribute(SESSION_CART_ID);
        if (cartId == null) {
            return "redirect:/cart.html";
        }
        String redirect = orderClientService.createOrder(cartId,
                        customerName, customerEmail,
                        shippingAddressLine1, shippingAddressLine2,
                        shippingCity, shippingState, shippingPostalCode, shippingCountry)
                .map(r -> {
                    cartClientService.createCart().ifPresent(newCartId ->
                            session.setAttribute(SESSION_CART_ID, newCartId));
                    return "redirect:/order-confirmation.html?orderId=" + r.orderId();
                })
                .orElse("redirect:/checkout.html");
        return redirect;
    }

    @GetMapping("/order-confirmation/{orderId}")
    public String orderConfirmationPage(@PathVariable Long orderId) {
        return "redirect:/order-confirmation.html?orderId=" + orderId;
    }

    @GetMapping("/order-status/{orderId}")
    @ResponseBody
    public ResponseEntity<OrderStatusResponse> orderStatus(@PathVariable Long orderId) {
        return orderClientService.getOrderStatus(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

**What it does:** GET /checkout sends the user to the checkout page. POST /checkout reads `cartId` from the session and customer/shipping fields from the form, calls the db to create an order, then **creates a new empty cart** and stores its id in the session so the user no longer sees the purchased items. Finally it redirects to the order-confirmation page with the new `orderId`. GET /order-confirmation/{orderId} redirects to the same confirmation page with that id. GET /order-status/{orderId} returns JSON for polling. The session key `cartId` must match the one used where the cart is stored (e.g. your existing cart controller).

---

## Part 13: Remove duplicate GET /checkout from CartController (web module)

If your existing `CartController` has a mapping for GET /checkout that redirects to the checkout page, remove it so that only `CheckoutController` handles /checkout.

**In:** `web/src/main/java/edu/byui/apj/storefront/web/controller/CartController.java`

**Remove these lines:**

```java
    @GetMapping("/checkout")
    public String checkoutPage() {
        return "redirect:/checkout.html";
    }

```

Keep the rest of `CartController` (cart page, api/cart, add/remove/update) unchanged.

**What it does:** Avoids two controllers mapping the same URL. Checkout is now fully handled by `CheckoutController`.

---

## Part 14: Properties (web module)

No new properties are required if your web module already configures the db service. Ensure you have:

**In:** `web/src/main/resources/application.properties`

```properties
db.service.base-url=http://localhost:8083
```

Use the port where your **db** application runs (e.g. 8083). The same WebClient is used for both cart and order APIs.

---

## Part 15: Static folder (instructor-provided)

The instructor will provide an updated **static folder** that includes:

- **checkout.html** – Checkout page with cart summary, total, **customer and shipping form fields** (name, email, address line 1, address line 2 optional, city, state, postal code, country), and a “Place Order” form that POSTs to `/checkout`. Form input `name` attributes must match the controller parameters: `customerName`, `customerEmail`, `shippingAddressLine1`, `shippingAddressLine2`, `shippingCity`, `shippingState`, `shippingPostalCode`, `shippingCountry`.
- **order-confirmation.html** – Order confirmation page that shows order id, current status, and a loading indicator, and polls `GET /order-status/{orderId}` (e.g. every 2 seconds) until status is `COMPLETED`, then shows “Order Complete” and stops polling.

After the user places an order, the server assigns a **new empty cart** to the session, so when they open the cart or checkout again they no longer see the purchased items.

Replace your `web/src/main/resources/static` folder with the provided one (or merge the new files) as directed by the instructor.

---

## Part 16: Run and test

1. Start the **db** module (e.g. port 8083).
2. Start the **web** module (e.g. port 8080).
3. Add items to the cart, go to checkout, and click “Place Order.”
4. You should be redirected to the order-confirmation page immediately (order is PENDING). After about 10 seconds, the status should change to COMPLETED and the page should update (if polling is implemented in the provided static files).
5. Without `@Async`, the POST /api/orders request would block for 10 seconds. With `@Async`, the response returns right away and the 10-second “payment” runs in the background.

---

## Summary

You added:

- **db module:** `OrderStatus` enum; `Order` and `OrderItem` entities (Order includes customer and shipping fields); repositories; `@EnableAsync`; `OrderProcessor` with `@Async` payment simulation (10 s sleep); `OrderService` to create orders from the cart and customer/shipping info and trigger async processing; `CreateOrderRequest` with cartId and customer/shipping fields; `OrderController` for POST /api/orders and GET /api/orders/{orderId}.
- **web module:** `OrderStatusResponse`; `OrderClientService` (createOrder with customer/shipping params, getOrderStatus); `CheckoutController` (GET/POST /checkout with customer and shipping request params, **new empty cart created after successful order** and stored in session, order-confirmation redirect, GET /order-status/{orderId}); removal of GET /checkout from `CartController`; existing `db.service.base-url` used for orders.

The UI talks only to the web module; the web module proxies to the db module. Customer and shipping data are captured at checkout and stored on the order. After placing an order, the user’s session is assigned a new empty cart so they no longer see the purchased items. Long-running work runs asynchronously in the db module so the user gets an immediate redirect and can poll until the order is complete.
