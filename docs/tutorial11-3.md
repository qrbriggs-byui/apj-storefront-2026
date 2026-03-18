# Tutorial 11-2: JMS Order Confirmation (DB-Triggered Producer)

This tutorial walks you through **event-driven order confirmation** using **JMS and Apache Artemis**. When the **db** module finishes async order processing (after the 10-second delay from Module 2), it publishes a small message to a queue. A separate **jms** application consumes that message, calls the db module for full order details, and prints a confirmation to the console.

This matches the architecture in **`docs/ORDER_CONFIRMATION_DB.md`**: the **db** module is the JMS **producer**; the **web** module does **not** send JMS messages.

You will practice:

- **JMS publish** from the service that owns “order completed”
- **JMS consume** in a dedicated Spring Boot app
- **Lightweight queue payloads** (order id + metadata) vs **source of truth** (REST to db)
- **Apache Artemis** as the message broker

Apply each step in your own codebase. Adjust package names and ports if yours differ.

---

## Prerequisites

- Working **async orders** (create order → `PENDING` → `PROCESSING` → ~10s → `COMPLETED`), as in `ASYNC_ORDER.md`.
- **Java 21** and **Maven**.
- **Apache Artemis** installed locally (see below).

### Install and start Apache Artemis (summary)

1. Download Apache Artemis from the [official site](https://activemq.apache.org/components/artemis/download/).
2. Create a broker, e.g. `bin/artemis create --user admin --password admin /path/to/mybroker`.
3. Start it: `mybroker/bin/artemis run`.
4. Default OpenWire/JMS port is often **61616**. The tutorials use user **`admin`** / password **`admin`**—change your `application.properties` if you use different credentials.

**Why Artemis must be running:** The db module connects as a **producer** when it sends a message; the jms module connects as a **consumer** on startup. If the broker is down, those apps may fail to start or log connection errors.

---

## Part 1: Register the `jms` module in the parent POM

**Purpose:** The consumer runs as its own Spring Boot application. Adding it to the parent build lets you run `mvn -pl jms` and keeps the multi-module project consistent.

**Step 1.1 —** In the **root** `pom.xml`, add `<module>jms</module>` inside `<modules>`:

```xml
<modules>
    <module>web</module>
    <module>api</module>
    <module>db</module>
    <module>api-mongo</module>
    <module>work</module>
    <module>jms</module>
</modules>
```

---

## Part 2: Add JMS producer dependencies and Artemis settings to the **db** module

**Purpose:** Only the db module knows exactly when an order becomes `COMPLETED`. Putting the producer here keeps the event aligned with your domain logic instead of relying on the browser polling the web tier.

**Step 2.1 —** In **`db/pom.xml`**, add the Artemis JMS starter next to your other starters:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-artemis</artifactId>
</dependency>
```

**Step 2.2 —** In **`db/src/main/resources/application.properties`**, append:

```properties
# Apache Artemis (JMS producer — order completed events)
spring.artemis.mode=native
spring.artemis.broker-url=tcp://localhost:61616
spring.artemis.user=admin
spring.artemis.password=admin
app.jms.order-confirmation-queue=order.confirmation
```

**Why:** `spring.artemis.*` connects Spring’s JMS support to your broker. `app.jms.order-confirmation-queue` is a single place to rename the queue for both db and jms modules.

---

## Part 3: Event DTO — `OrderCompletedMessage` (db module)

**Purpose:** The queue carries a **small JSON document** (event type, order id, timestamp). The consumer uses the id to load the real order from the db API—teaching “event notification vs source of truth.”

**Step 3.1 —** Create **`db/src/main/java/.../db/messaging/OrderCompletedMessage.java`**:

```java
package edu.byui.apj.storefront.db.messaging;

import java.time.Instant;

/**
 * Lightweight event placed on the JMS queue when an order finishes processing.
 * Full order data is fetched later via REST by the consumer.
 */
public record OrderCompletedMessage(String eventType, Long orderId, Instant completedAt) {

    public static OrderCompletedMessage orderCompleted(Long orderId) {
        return new OrderCompletedMessage("ORDER_COMPLETED", orderId, Instant.now());
    }
}
```

---

## Part 4: Producer — `OrderConfirmationProducer` (db module)

**Purpose:** Encapsulates “send one text message with JSON to the queue.” Using `JmsTemplate` and Jackson keeps the code short; failures are logged so a broker outage does not roll back an already-completed order.

**Step 4.1 —** Create **`db/src/main/java/.../db/messaging/OrderConfirmationProducer.java`**:

```java
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
```

**Why serialize before `send`:** The JMS callback may only throw `JMSException`; writing JSON outside the lambda avoids checked-exception issues.

---

## Part 5: Trigger the message when processing completes — `OrderProcessor`

**Purpose:** This is the **only** place that should publish the event for a successful order: right after the async work sets status to `COMPLETED` and saves. No message is sent on `FAILED` or before the delay.

**Step 5.1 —** Update **`OrderProcessor`** to inject the producer and call it after completion. Example full class:

```java
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
```

---

## Part 6: Order details API for the consumer (db module)

**Purpose:** The jms app must not rely on the queue for line items and totals. A dedicated **details** endpoint returns everything needed to build a human-readable confirmation.

**Step 6.1 —** Create **`OrderDetailsItemResponse.java`** in `controller/dto`:

```java
package edu.byui.apj.storefront.db.controller.dto;

public record OrderDetailsItemResponse(
        String productId,
        String productName,
        int quantity,
        double price
) {}
```

**Step 6.2 —** Create **`OrderDetailsResponse.java`**:

```java
package edu.byui.apj.storefront.db.controller.dto;

import java.time.Instant;
import java.util.List;

public record OrderDetailsResponse(
        Long orderId,
        Instant createdAt,
        String status,
        double totalAmount,
        Long cartId,
        List<OrderDetailsItemResponse> items
) {}
```

**Step 6.3 —** In **`OrderService`**, add a method that loads the order, touches `items` so JPA loads them, and maps to the DTOs:

```java
import edu.byui.apj.storefront.db.controller.dto.OrderDetailsItemResponse;
import edu.byui.apj.storefront.db.controller.dto.OrderDetailsResponse;
import java.util.List;

// ...

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
```

**Step 6.4 —** In **`OrderController`**, add:

```java
import edu.byui.apj.storefront.db.controller.dto.OrderDetailsResponse;

// ...

@GetMapping("/{orderId}/details")
public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable Long orderId) {
    return ResponseEntity.ok(orderService.getOrderDetails(orderId));
}
```

**Test:** `GET http://localhost:8083/api/orders/1/details` (use a real order id after placing an order).

---

## Part 7: Create the **jms** Maven module

**Purpose:** A minimal app that only listens to the queue and calls the db service—mirroring real “notification worker” services.

**Step 7.1 —** Create folder **`jms/`** at the project root.

**Step 7.2 —** Create **`jms/pom.xml`**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>edu.byui.apj.storefront</groupId>
        <artifactId>parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <artifactId>jms</artifactId>
    <name>jms</name>
    <description>JMS consumer for order confirmation messages (Apache Artemis)</description>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-artemis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Why WebFlux:** `WebClient` is a simple way to call `GET /api/orders/{id}/details` without pulling in full MVC.

---

## Part 8: JMS application entry point and properties

**Step 8.1 —** **`jms/src/main/resources/application.properties`**:

```properties
spring.application.name=jms
server.port=8085

# Apache Artemis (same broker and queue as db producer)
spring.artemis.mode=native
spring.artemis.broker-url=tcp://localhost:61616
spring.artemis.user=admin
spring.artemis.password=admin
app.jms.order-confirmation-queue=order.confirmation

# DB module REST API (source of truth for order details)
db.service.base-url=http://localhost:8083
```

**Step 8.2 —** **`jms/src/main/java/.../jms/JmsApplication.java`**:

```java
package edu.byui.apj.storefront.jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class JmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmsApplication.class, args);
    }
}
```

**Why `@EnableJms`:** Registers listener endpoints for `@JmsListener`.

---

## Part 9: DTOs in the **jms** module

**Purpose:** Same JSON shape as the db API and the queue payload. Duplicating small DTOs avoids a shared library for this lab.

**Step 9.1 —** **`jms/.../dto/OrderCompletedMessage.java`**:

```java
package edu.byui.apj.storefront.jms.dto;

import java.time.Instant;

/** Mirrors the JSON payload produced by the db module. */
public record OrderCompletedMessage(String eventType, Long orderId, Instant completedAt) {}
```

**Step 9.2 —** **`OrderDetailsItemDto.java`**:

```java
package edu.byui.apj.storefront.jms.dto;

public record OrderDetailsItemDto(String productId, String productName, int quantity, double price) {}
```

**Step 9.3 —** **`OrderDetailsDto.java`**:

```java
package edu.byui.apj.storefront.jms.dto;

import java.time.Instant;
import java.util.List;

public record OrderDetailsDto(
        Long orderId,
        Instant createdAt,
        String status,
        double totalAmount,
        Long cartId,
        List<OrderDetailsItemDto> items
) {}
```

---

## Part 10: WebClient to call the db module

**Step 10.1 —** **`jms/.../config/DbServiceWebClientConfig.java`**:

```java
package edu.byui.apj.storefront.jms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DbServiceWebClientConfig {

    @Bean
    public WebClient dbServiceWebClient(@Value("${db.service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
```

**Step 10.2 —** **`jms/.../client/OrderDetailsClient.java`**:

```java
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
```

**Why `.block()`:** The listener runs on a JMS thread; blocking one call is acceptable for a teaching app. Production code might use reactive end-to-end or virtual threads.

---

## Part 11: Formatting the confirmation text

**Step 11.1 —** **`jms/.../service/OrderConfirmationMessageService.java`**:

```java
package edu.byui.apj.storefront.jms.service;

import edu.byui.apj.storefront.jms.dto.OrderDetailsDto;
import edu.byui.apj.storefront.jms.dto.OrderDetailsItemDto;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class OrderConfirmationMessageService {

    public String buildConfirmationMessage(OrderDetailsDto details) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order Confirmation\n");
        sb.append("Order ID: ").append(details.orderId()).append('\n');
        sb.append("Status: ").append(details.status()).append('\n');
        sb.append(String.format(Locale.US, "Total: $%.2f\n", details.totalAmount()));
        sb.append("Items:\n");
        for (OrderDetailsItemDto line : details.items()) {
            double lineTotal = line.price() * line.quantity();
            sb.append(String.format(Locale.US, "- %s x%d - $%.2f\n",
                    line.productName(), line.quantity(), lineTotal));
        }
        sb.append("Thank you for your order!");
        return sb.toString();
    }
}
```

---

## Part 12: JMS listener

**Purpose:** Tie together parse → fetch → format → output. `System.out.println` makes the confirmation obvious in the console during demos.

**Step 12.1 —** **`jms/.../listener/OrderConfirmationListener.java`**:

```java
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
```

**Note:** Rethrowing may cause Artemis to **redeliver** the message—useful to discuss in class; production systems often use dead-letter queues and idempotent handling.

---

## Part 13: Optional unit test (jms module)

**Purpose:** Test the formatter without starting Artemis or the db module.

**Step 13.1 —** **`jms/src/test/java/.../service/OrderConfirmationMessageServiceTest.java`**:

```java
package edu.byui.apj.storefront.jms.service;

import edu.byui.apj.storefront.jms.dto.OrderDetailsDto;
import edu.byui.apj.storefront.jms.dto.OrderDetailsItemDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderConfirmationMessageServiceTest {

    @Test
    void buildConfirmationMessage_formatsOrder() {
        OrderConfirmationMessageService svc = new OrderConfirmationMessageService();
        OrderDetailsDto dto = new OrderDetailsDto(
                1L,
                Instant.parse("2026-01-01T12:00:00Z"),
                "COMPLETED",
                49.97,
                10L,
                List.of(
                        new OrderDetailsItemDto("p1", "Blue Eyes White Dragon", 1, 19.99),
                        new OrderDetailsItemDto("p2", "Dark Magician", 1, 29.98)));
        String msg = svc.buildConfirmationMessage(dto);
        assertThat(msg).contains("Order ID: 1");
        assertThat(msg).contains("COMPLETED");
        assertThat(msg).contains("$49.97");
        assertThat(msg).contains("Blue Eyes White Dragon");
        assertThat(msg).contains("Thank you for your order!");
    }
}
```

---

## Part 14: Db module tests without a running broker

**Purpose:** Spring Boot still auto-configures JMS when Artemis is on the classpath. For **`@SpringBootTest`**, you can replace the producer with a mock so tests do not require a broker.

**Step 14.1 —** In **`DbApplicationTests`**:

```java
package edu.byui.apj.storefront.db;

import edu.byui.apj.storefront.db.messaging.OrderConfirmationProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class DbApplicationTests {

    @MockBean
    OrderConfirmationProducer orderConfirmationProducer;

    @Test
    void contextLoads() {
    }
}
```

---

## Part 15: Run everything end-to-end

1. **Start Artemis** (`artemis run` for your broker).
2. **Start the db module** (port **8083** in this project).
3. **Start the jms module** (port **8085**).
4. **Start the web module** if you use the browser checkout.
5. **Place an order** and wait **at least 10 seconds** after submission.

**What you should see:**

- Db logs: order completed and `ORDER_COMPLETED event sent to queue order.confirmation`.
- Jms console: multi-line **Order Confirmation** block with items and total.

**Order matters:** If jms starts **after** the message was sent but **before** the consumer processed it, Artemis typically **holds** the message until a consumer connects—good demo of decoupling.

**If jms is down when the message is sent:** The db module still completes the order; the producer logs an error if the broker is unreachable. If the broker is up but jms is off, messages wait on the queue until jms starts.

---

## Summary

| Location | What you added |
|----------|----------------|
| **Parent `pom.xml`** | `<module>jms</module>` |
| **db** | `spring-boot-starter-artemis`, Artemis properties, `OrderCompletedMessage`, `OrderConfirmationProducer`, `OrderProcessor` calls producer after `COMPLETED`, `OrderDetailsItemResponse` / `OrderDetailsResponse`, `OrderService.getOrderDetails`, `GET /api/orders/{id}/details` |
| **jms** | New app: `JmsApplication`, DTOs, `DbServiceWebClientConfig`, `OrderDetailsClient`, `OrderConfirmationMessageService`, `OrderConfirmationListener`, `application.properties` |
| **Tests** | `OrderConfirmationMessageServiceTest`; `@MockBean OrderConfirmationProducer` on db context test |

**Concepts:**

- **Producer at the source of truth** for the lifecycle event (db after async completion).
- **Thin events** on the bus; **rich data** via HTTP to the owning service.
- **Third process (jms)** as a stand-in for email/SMS/fulfillment subscribers.

For the alternate design where the **web** module publishes after polling sees `COMPLETED`, see **`docs/ORDER_CONFIRMATION.md`**.

---

*End of Tutorial 11-2*
