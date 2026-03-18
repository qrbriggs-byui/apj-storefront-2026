# Tutorial 11-2: Scheduled Cart Cleanup (Module 3)

This tutorial walks you through implementing **scheduled cart cleanup** in the storefront: a background job that runs on a timer and removes carts older than a configurable age. You will add a new **work** module (scheduler + reporting) and extend the **db** module with a cleanup API. By the end, you will have applied:

- **Spring scheduling** (`@Scheduled`, `@EnableScheduling`)
- **Externalized configuration** (`application.properties`, `@ConfigurationProperties`)
- **Service-to-service REST** (work module calling db module via WebClient)
- **Separation of concerns** (worker vs persistence)

Apply each step in your own codebase, using the same package names and structure if they match, or adjust paths to fit your project.

---

## Part 1: Add the work module to the parent project

**Purpose:** The cleanup job lives in its own Spring Boot application (the **work** module) so that background work is separate from the web and db services. This keeps the architecture clear and mirrors real systems where workers are often separate processes.

**Step 1.1 —** Open the **parent** `pom.xml` (root of the multi-module project). Add the `work` module to the `<modules>` list.

```xml
<modules>
    <module>web</module>
    <module>api</module>
    <module>db</module>
    <module>api-mongo</module>
    <module>work</module>
</modules>
```

**Why:** Maven will now build and recognize the `work` module when you run builds from the root.

---

## Part 2: Create the work module structure

**Purpose:** The work module is a minimal Spring Boot app that only runs the scheduled job and calls the db module. It needs its own `pom.xml`, main class, and configuration.

**Step 2.1 —** Create the work module directory: `work/` at the same level as `web/`, `db/`, etc.

**Step 2.2 —** Create `work/pom.xml` with the following content. Use your parent `groupId`, `artifactId`, and `version` to match the rest of the project.

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
    <artifactId>work</artifactId>
    <name>work</name>
    <description>Background worker module for scheduled cart cleanup</description>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
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

**Why:** `spring-boot-starter-web` provides the Spring context and scheduling; `spring-boot-starter-webflux` provides WebClient for calling the db module. We do not need JPA or a database in the work module.

**Step 2.3 —** Create `work/src/main/resources/application.properties`:

```properties
spring.application.name=work
server.port=8084

# Cart cleanup: age threshold (minutes) and how often the job runs (ms)
cart.cleanup.max-age-minutes=30
cart.cleanup.fixed-delay-ms=60000

# DB module base URL for cleanup API
db.service.base-url=http://localhost:8083
```

**Why:** All cleanup behavior is driven by configuration. You can change the expiration age and the delay between runs without recompiling. Use the port where your **db** module runs (e.g. 8083); adjust if your db runs elsewhere.

---

## Part 3: Ensure the Cart entity has `createdAt`

**Purpose:** Cleanup identifies “expired” carts by creation time. The Cart entity must have a `createdAt` field, and it must be set when a cart is created.

**Step 3.1 —** In the **db** module, open your `Cart` entity. Ensure it has a `createdAt` field (e.g. `Instant` or `LocalDateTime`). Example:

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue
    private Long id;

    private Instant createdAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();
}
```

**Step 3.2 —** In your `CartService` (or wherever carts are created), set `createdAt` when creating a new cart:

```java
public Cart createCart() {
    Cart cart = new Cart();
    cart.setCreatedAt(Instant.now());
    return cartRepository.save(cart);
}
```

**Why:** Without a populated `createdAt`, we cannot compute which carts are older than the configured threshold.

---

## Part 4: Extend CartRepository for cleanup

**Purpose:** The db module needs to find all carts created before a cutoff time so it can delete them. A single repository method keeps the query in one place and is easy to test.

**Step 4.1 —** In the **db** module, open `CartRepository`. Add a method that finds carts by creation time:

```java
package edu.byui.apj.storefront.db.repository;

import edu.byui.apj.storefront.db.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find carts created before the given cutoff time (for cleanup of expired carts).
     */
    List<Cart> findByCreatedAtBefore(Instant cutoff);
}
```

**Why:** Spring Data JPA derives the query from the method name. `findByCreatedAtBefore(Instant cutoff)` generates the appropriate SQL. We use `Instant` to match the Cart entity; use `LocalDateTime` if your entity uses that instead.

---

## Part 5: Add the cleanup response DTO in the db module

**Purpose:** The cleanup API returns a small summary (how many carts were removed, cutoff time, etc.) so the work module can log a clear report. A DTO keeps the API contract explicit.

**Step 5.1 —** In the **db** module, create a new DTO in your controller/dto package (e.g. `db/.../controller/dto/CartCleanupResponse.java`):

```java
package edu.byui.apj.storefront.db.controller.dto;

import java.time.Instant;

/**
 * Summary returned after running cart cleanup. Used by the work module for reporting.
 */
public record CartCleanupResponse(
        int removedCount,
        int maxAgeMinutes,
        Instant cutoffTime,
        Instant runAt
) {}
```

**Why:** Records are a concise way to define immutable DTOs. The work module will deserialize this from JSON and use it to build the end-of-run report.

---

## Part 6: Implement CartCleanupService in the db module

**Purpose:** The service encapsulates the cleanup logic: compute cutoff, load expired carts, log each one, delete them, and return the summary. Keeping this in the db module keeps persistence and logging in one place.

**Step 6.1 —** In the **db** module, create `service/CartCleanupService.java`:

```java
package edu.byui.apj.storefront.db.service;

import edu.byui.apj.storefront.db.controller.dto.CartCleanupResponse;
import edu.byui.apj.storefront.db.model.Cart;
import edu.byui.apj.storefront.db.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CartCleanupService {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupService.class);

    private final CartRepository cartRepository;

    public CartCleanupService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional
    public CartCleanupResponse cleanupExpiredCarts(int maxAgeMinutes) {
        Instant runAt = Instant.now();
        Instant cutoff = runAt.minusSeconds(maxAgeMinutes * 60L);

        List<Cart> expired = cartRepository.findByCreatedAtBefore(cutoff);
        for (Cart cart : expired) {
            log.info("Removing expired cart id={} createdAt={}", cart.getId(), cart.getCreatedAt());
            cartRepository.delete(cart);
        }

        return new CartCleanupResponse(
                expired.size(),
                maxAgeMinutes,
                cutoff,
                runAt
        );
    }
}
```

**Why:**  
- `@Transactional` ensures all deletes run in one transaction.  
- We load carts first, log each, then delete one by one so each removed cart appears in the db logs (good for teaching and operations).  
- The response carries everything the work module needs for its report.

---

## Part 7: Add the cleanup REST endpoint in the db module

**Purpose:** The work module triggers cleanup by calling a REST endpoint. Using an “internal” path (`/api/internal/...`) makes it clear this is for backend workers, not the browser.

**Step 7.1 —** In the **db** module, create `controller/CartCleanupController.java`:

```java
package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.controller.dto.CartCleanupResponse;
import edu.byui.apj.storefront.db.service.CartCleanupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/cart-cleanup")
public class CartCleanupController {

    private final CartCleanupService cleanupService;

    public CartCleanupController(CartCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @PostMapping
    public ResponseEntity<CartCleanupResponse> cleanup(
            @RequestParam(name = "maxAgeMinutes", defaultValue = "30") int maxAgeMinutes) {
        if (maxAgeMinutes <= 0) {
            return ResponseEntity.badRequest().build();
        }
        CartCleanupResponse response = cleanupService.cleanupExpiredCarts(maxAgeMinutes);
        return ResponseEntity.ok(response);
    }
}
```

**Why:**  
- **POST** is used because cleanup is an action that may delete many resources, not a single resource DELETE.  
- **Query parameter** `maxAgeMinutes` keeps the API simple and easy to call from WebClient.  
- **Validation** (`maxAgeMinutes <= 0` → 400) teaches that internal APIs should still validate input.

**Testing:** With the db application running, you can call:

```http
POST http://localhost:8083/api/internal/cart-cleanup?maxAgeMinutes=30
```

You should get a JSON response with `removedCount`, `maxAgeMinutes`, `cutoffTime`, and `runAt`.

---

## Part 8: Create the work module application and configuration

**Purpose:** The work module must be a runnable Spring Boot application with scheduling enabled and cleanup settings bound to a typed class.

**Step 8.1 —** Create the main class `work/.../WorkApplication.java` (adjust package to match your project):

```java
package edu.byui.apj.storefront.work;

import edu.byui.apj.storefront.work.config.CartCleanupProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CartCleanupProperties.class)
public class WorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkApplication.class, args);
    }
}
```

**Why:**  
- `@EnableScheduling` allows `@Scheduled` methods to run.  
- `@EnableConfigurationProperties(CartCleanupProperties.class)` registers the configuration class so `cart.cleanup.*` properties are available in the job.

**Step 8.2 —** Create the configuration properties class `work/.../config/CartCleanupProperties.java`:

```java
package edu.byui.apj.storefront.work.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cart.cleanup")
public class CartCleanupProperties {

    private int maxAgeMinutes = 30;
    private long fixedDelayMs = 60000;

    public int getMaxAgeMinutes() {
        return maxAgeMinutes;
    }

    public void setMaxAgeMinutes(int maxAgeMinutes) {
        this.maxAgeMinutes = maxAgeMinutes;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }
}
```

**Why:** Typed configuration is easier to use and document than raw `@Value` in several places. The prefix `cart.cleanup` matches the properties in `application.properties`.

**Step 8.3 —** Create the WebClient config for the db service `work/.../config/WorkWebClientConfig.java`:

```java
package edu.byui.apj.storefront.work.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WorkWebClientConfig {

    @Value("${db.service.base-url}")
    private String dbServiceBaseUrl;

    @Bean
    public WebClient dbServiceClient() {
        return WebClient.builder()
                .baseUrl(dbServiceBaseUrl)
                .build();
    }
}
```

**Why:** The base URL comes from configuration, so you can point the work module at different environments without code changes.

---

## Part 9: Add the CartCleanupResponse DTO in the work module

**Purpose:** The work module needs a local DTO that matches the JSON returned by the db module. Duplicating the DTO in the work module keeps the module independent (no shared JAR needed for this tutorial).

**Step 9.1 —** In the **work** module, create `dto/CartCleanupResponse.java` (same shape as the db module’s DTO):

```java
package edu.byui.apj.storefront.work.dto;

import java.time.Instant;

public record CartCleanupResponse(
        int removedCount,
        int maxAgeMinutes,
        Instant cutoffTime,
        Instant runAt
) {}
```

**Why:** WebClient will deserialize the response into this record. Field names and types must match the JSON from the db module.

---

## Part 10: Implement the client that calls the db cleanup API

**Purpose:** The scheduled job should not contain HTTP code. A dedicated client service keeps the job simple and makes the call easy to test or reuse.

**Step 10.1 —** In the **work** module, create `client/CartCleanupClientService.java`:

```java
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
```

**Why:**  
- WebClient matches the pattern used in the web module for calling other services.  
- `@Qualifier("dbServiceClient")` selects the bean from `WorkWebClientConfig`.  
- `.block()` is acceptable here because the scheduled job is already running in a background thread; we keep the implementation simple.

---

## Part 11: Implement the scheduled cleanup job

**Purpose:** This is the component that runs on a fixed delay, reads configuration, calls the db cleanup API, and logs the report. Wrapping the call in try/catch prevents one failed run from breaking the scheduler.

**Step 11.1 —** In the **work** module, create `job/CartCleanupJob.java`:

```java
package edu.byui.apj.storefront.work.job;

import edu.byui.apj.storefront.work.client.CartCleanupClientService;
import edu.byui.apj.storefront.work.config.CartCleanupProperties;
import edu.byui.apj.storefront.work.dto.CartCleanupResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CartCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupJob.class);

    private final CartCleanupProperties properties;
    private final CartCleanupClientService client;

    public CartCleanupJob(CartCleanupProperties properties, CartCleanupClientService client) {
        this.properties = properties;
        this.client = client;
    }

    @Scheduled(fixedDelayString = "${cart.cleanup.fixed-delay-ms}")
    public void runCartCleanup() {
        log.info("Starting cart cleanup job with maxAgeMinutes={}", properties.getMaxAgeMinutes());
        try {
            CartCleanupResponse response = client.triggerCleanup(properties.getMaxAgeMinutes());
            log.info("Cart cleanup run complete. Run time: {}, Expiration threshold: {} minutes, Removed carts: {}, Cutoff used: {}",
                    response.runAt(),
                    response.maxAgeMinutes(),
                    response.removedCount(),
                    response.cutoffTime());
        } catch (Exception ex) {
            log.error("Cart cleanup job failed", ex);
        }
    }
}
```

**Why:**  
- `@Scheduled(fixedDelayString = "${cart.cleanup.fixed-delay-ms}")` runs the method repeatedly, with a delay *after* each run finishes (e.g. every 60 seconds after the previous run completes).  
- Reading `maxAgeMinutes` from `CartCleanupProperties` keeps the job in sync with `application.properties`.  
- The try/catch ensures that if the db module is down, the work application logs the error and continues; the next run will try again.

---

## Part 12: Add a simple test for the work application

**Purpose:** A minimal test that loads the Spring context verifies that the work module starts without missing beans or misconfiguration.

**Step 12.1 —** Create `work/src/test/java/.../WorkApplicationTests.java` (package same as `WorkApplication`):

```java
package edu.byui.apj.storefront.work;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

**Why:** If you add or change beans later, this test will catch configuration errors at build time.

---

## Part 13: Verify the full flow

**Purpose:** Confirm that the scheduler runs, calls the db module, and that logs appear in both applications.

1. **Start the db module** (e.g. run `DbApplication` or `mvn spring-boot:run -pl db`). Note the port (e.g. 8083) and ensure `db.service.base-url` in the work module matches.

2. **Start the work module** (e.g. run `WorkApplication` or `mvn spring-boot:run -pl work`).

3. **Create some carts** via your web UI or the db API. Optionally, insert or backdate carts in the database so some are older than `cart.cleanup.max-age-minutes` (for a quick test, set `cart.cleanup.max-age-minutes=1` and `cart.cleanup.fixed-delay-ms=10000` in the work module’s `application.properties`).

4. **Watch the logs:**  
   - **Work module:** You should see “Starting cart cleanup job…” and “Cart cleanup run complete…” with the summary.  
   - **Db module:** You should see “Removing expired cart id=… createdAt=…” for each deleted cart.

5. **Optional:** Call the cleanup endpoint manually to verify the API:

   ```http
   POST http://localhost:8083/api/internal/cart-cleanup?maxAgeMinutes=30
   ```

---

## Summary of what you built

| Location | What you added |
|----------|----------------|
| **Parent pom** | `<module>work</module>` |
| **work module** | New Spring Boot app: `WorkApplication`, `CartCleanupProperties`, `WorkWebClientConfig`, `CartCleanupResponse` (DTO), `CartCleanupClientService`, `CartCleanupJob`, `application.properties` |
| **db module** | `CartRepository.findByCreatedAtBefore(Instant)`, `CartCleanupResponse` (DTO), `CartCleanupService`, `CartCleanupController` (POST `/api/internal/cart-cleanup?maxAgeMinutes=...`) |

Concepts you used:

- **`@EnableScheduling`** and **`@Scheduled(fixedDelayString = "...")`** for recurring background work.
- **`@ConfigurationProperties`** for typed, externalized configuration.
- **WebClient** for the work module to call the db module’s REST API.
- **Separation of roles:** work = scheduling and reporting; db = persistence and cleanup logic.
- **Operational logging:** db logs each deleted cart; work logs the run summary.

You can now adapt this pattern to other scheduled tasks (e.g. order timeout, cache refresh) in your own projects.
