# Tutorial 10-1a: Implementing the Shopping Cart Feature

This tutorial gives you the code to add shopping cart functionality to the APJ storefront. Copy each file or code block into your project. Brief explanations follow each part.

**Architecture:** Browser → web module (controllers + CartClientService) → db module (REST API) → database. The web module never talks to the database directly; it calls the db module over HTTP with WebClient.

---

## Part 1: DB Module — DTOs and API

### 1.1 CartItemDTO (db module)

Create this file in your db module under the controller dto package (e.g. `db/src/main/java/edu/byui/apj/storefront/db/controller/dto/CartItemDTO.java`).

**What it does:** Represents one line item in the cart in the API response. Using a DTO keeps your REST contract stable and avoids exposing JPA entities.

```java
package edu.byui.apj.storefront.db.controller.dto;

/**
 * DTO for a single cart line item. Exposed by the cart API to avoid leaking JPA entities.
 */
public record CartItemDTO(
        Long id,
        String productId,
        String productName,
        int quantity,
        double price
) {}
```

---

### 1.2 CartDTO (db module)

Create `db/src/main/java/edu/byui/apj/storefront/db/controller/dto/CartDTO.java`.

**What it does:** Represents the whole cart (id, creation date, and list of items) in API responses. `createdAt` is serialized as ISO-8601 in JSON.

```java
package edu.byui.apj.storefront.db.controller.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for the cart resource. Exposed by the cart API to avoid leaking JPA entities.
 */
public record CartDTO(
        Long id,
        Instant createdAt,
        List<CartItemDTO> items
) {}
```

---

### 1.2a Cart entity — add createdAt (db module)

In your `Cart` entity (e.g. `db/.../model/Cart.java`), add a creation timestamp so the API can expose when the cart was created.

**Add the import:**
```java
import java.time.Instant;
```

**Add the field** (e.g. after `id`, before `items`):
```java
    private Instant createdAt;
```

**What it does:** Persists the cart creation time. The service will set it when creating a new cart, and the controller will map it into `CartDTO`.

---

### 1.3 AddItemRequest — use String productId (db module)

Your request body for adding an item should use `String productId` so you can support both numeric IDs (from a DB) and string IDs (e.g. from Mongo). Replace or create `AddItemRequest` in the db controller dto package:

```java
package edu.byui.apj.storefront.db.controller.dto;

public record AddItemRequest(
        String productId,
        String name,
        Double price,
        Integer quantity
) {}
```

---

### 1.4 Item entity — change productId to String (db module)

In your `Item` entity (e.g. `db/.../model/Item.java`), change the product id field to `String`:

**Change this line:**
```java
private Long productId;
```
**To:**
```java
private String productId;
```

If your entity uses a different name (e.g. `productName` for the display name), keep that; the controller will map `name` to `productName` in the DTO.

---

### 1.5 CartService — String productId, createCart with createdAt, and clearCart (db module)

In `CartService`:

1. Update the signature of `addItemToCart` to accept `String productId` (replace `Long productId` with `String productId` in the method parameter).
2. Replace `createCart()` so that new carts get a creation timestamp. Add `import java.time.Instant;` and use:

```java
    public Cart createCart() {
        Cart cart = new Cart();
        cart.setCreatedAt(Instant.now());
        return cartRepository.save(cart);
    }
```

3. Add this method at the end of the class (before the closing `}`):

```java
    public void clearCart(Long cartId) {
        Cart cart = getCart(cartId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
```

**What it does:** New carts get `createdAt` set so the API can return it. `clearCart` empties the cart so the “clear cart” API can remove all items without deleting the cart resource.

---

### 1.6 CartController (db module) — full replacement

Replace the contents of your db module’s `CartController` with the following. The base path becomes `/api/cart`, and every response uses the DTOs instead of entities. **Path:** `db/src/main/java/edu/byui/apj/storefront/db/controller/CartController.java`

```java
package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.controller.dto.AddItemRequest;
import edu.byui.apj.storefront.db.controller.dto.CartDTO;
import edu.byui.apj.storefront.db.controller.dto.CartItemDTO;
import edu.byui.apj.storefront.db.model.Cart;
import edu.byui.apj.storefront.db.model.Item;
import edu.byui.apj.storefront.db.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CartDTO> create(UriComponentsBuilder uriBuilder) {
        Cart cart = service.createCart();
        var location = uriBuilder.path("/api/cart/{id}").buildAndExpand(cart.getId()).toUri();
        return ResponseEntity.created(location).body(toCartDTO(cart));
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartDTO> get(@PathVariable Long cartId) {
        Cart cart = service.getCart(cartId);
        return ResponseEntity.ok(toCartDTO(cart));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartItemDTO> addItem(@PathVariable("cartId") Long cartId,
                                               @RequestBody AddItemRequest req,
                                               UriComponentsBuilder uriBuilder) {
        Item created = service.addItemToCart(cartId, req.productId(), req.name(), req.price(), req.quantity());
        var location = uriBuilder.path("/api/cart/{cartId}/items/{itemId}")
                .buildAndExpand(cartId, created.getId()).toUri();
        return ResponseEntity.created(location).body(toItemDTO(created));
    }

    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartItemDTO> updateItem(@PathVariable Long cartId,
                                                  @PathVariable Long itemId,
                                                  @RequestBody AddItemRequest req) {
        int qty = req.quantity() != null ? req.quantity() : 1;
        Item updated = service.updateItem(cartId, itemId, qty);
        return ResponseEntity.ok(toItemDTO(updated));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long cartId, @PathVariable Long itemId) {
        service.removeItem(cartId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long cartId) {
        service.clearCart(cartId);
        return ResponseEntity.noContent().build();
    }

    private static CartDTO toCartDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream()
                .map(CartController::toItemDTO)
                .collect(Collectors.toList());
        return new CartDTO(cart.getId(), cart.getCreatedAt(), items);
    }

    private static CartItemDTO toItemDTO(Item item) {
        return new CartItemDTO(
                item.getId(),
                item.getProductId(),
                item.getName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
```

**What it does:** Exposes the cart REST API at `/api/cart`: create cart, get cart, add/update/remove items, clear cart. All responses are DTOs. The `toCartDTO` and `toItemDTO` helpers keep entity-to-DTO mapping in one place.

---

## Part 2: Web Module — Configuration and Cart Client

### 2.1 application.properties — db service URL (web module)

Add this line to your web module’s `application.properties` (use the port where your db app runs, e.g. 8083):

```properties
db.service.base-url=http://localhost:8083
```

**What it does:** Tells the web app where to find the db module’s REST API.

---

### 2.2 WebClientConfig — db client bean (web module)

In your `WebClientConfig` class, add the following. If you already have other `@Value` and `@Bean` methods, add only the **db service** block (the last `@Value` and the `dbServiceClient` bean).

```java
    @Value("${db.service.base-url}")
    private String dbServiceBaseUrl;

    @Bean
    public WebClient dbServiceClient() {
        return WebClient.builder()
                .baseUrl(dbServiceBaseUrl)
                .build();
    }
```

**What it does:** Registers a dedicated WebClient that uses the db service base URL. The web module will inject this bean when calling the cart API.

---

### 2.3 CartItemDTO (web module)

Create `web/src/main/java/edu/byui/apj/storefront/web/model/CartItemDTO.java`. Field names must match the JSON from the db API so WebClient can deserialize.

```java
package edu.byui.apj.storefront.web.model;

/**
 * Web-layer DTO for a cart line item. Matches the structure returned by the db cart API.
 */
public record CartItemDTO(
        Long id,
        String productId,
        String productName,
        int quantity,
        double price
) {}
```

---

### 2.4 CartDTO (web module)

Create `web/src/main/java/edu/byui/apj/storefront/web/model/CartDTO.java`. Field names must match the db API JSON (including `createdAt`).

```java
package edu.byui.apj.storefront.web.model;

import java.time.Instant;
import java.util.List;

/**
 * Web-layer DTO for the cart. Matches the structure returned by the db cart API.
 */
public record CartDTO(
        Long id,
        Instant createdAt,
        List<CartItemDTO> items
) {}
```

---

### 2.5 CartClientService (web module)

Create `web/src/main/java/edu/byui/apj/storefront/web/service/CartClientService.java`:

```java
package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.CartDTO;
import edu.byui.apj.storefront.web.model.CartItemDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Calls the db module cart REST API via WebClient. Converts responses to web DTOs.
 */
@Service
public class CartClientService {

    private final WebClient webClient;

    public CartClientService(@Qualifier("dbServiceClient") WebClient dbServiceClient) {
        this.webClient = dbServiceClient;
    }

    public Optional<CartDTO> getCart(Long cartId) {
        return webClient.get()
                .uri("/api/cart/{cartId}", cartId)
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.error(new RuntimeException("Cart API error: " + response.statusCode())))
                .bodyToMono(CartDTO.class)
                .map(Optional::of)
                .onErrorReturn(Optional.empty())
                .block();
    }

    public Optional<Long> createCart() {
        CartDTO created = webClient.post()
                .uri("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(CartDTO.class)
                .block();
        return created != null ? Optional.of(created.id()) : Optional.empty();
    }

    public Optional<CartItemDTO> addItem(Long cartId, String productId, String productName, double price, int quantity) {
        Map<String, Object> body = Map.of(
                "productId", productId != null ? productId : "",
                "name", productName != null ? productName : "",
                "price", price,
                "quantity", quantity
        );
        CartItemDTO item = webClient.post()
                .uri("/api/cart/{cartId}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CartItemDTO.class)
                .block();
        return Optional.ofNullable(item);
    }

    public boolean updateItem(Long cartId, Long itemId, int quantity) {
        try {
            webClient.put()
                    .uri("/api/cart/{cartId}/items/{itemId}", cartId, itemId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeItem(Long cartId, Long itemId) {
        try {
            webClient.delete()
                    .uri("/api/cart/{cartId}/items/{itemId}", cartId, itemId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clearCart(Long cartId) {
        try {
            webClient.delete()
                    .uri("/api/cart/{cartId}", cartId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

**What it does:** Single place in the web module that talks to the db cart API: get cart, create cart, add/update/remove items, clear cart. Controllers will use this service instead of doing HTTP themselves.

---

## Part 3: Web Module — Cart Controller and Session

### 3.1 CartController (web module)

Create `web/src/main/java/edu/byui/apj/storefront/web/controller/CartController.java` (this is the **web** controller, not the db one):

```java
package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.CartDTO;
import edu.byui.apj.storefront.web.service.CartClientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Handles cart page rendering, cart actions (add/remove/update), and checkout navigation.
 * Uses HTTP session to store cartId. Delegates persistence to CartClientService (db module via WebClient).
 */
@Controller
public class CartController {

    private static final String SESSION_CART_ID = "cartId";

    private final CartClientService cartClientService;

    public CartController(CartClientService cartClientService) {
        this.cartClientService = cartClientService;
    }

    @GetMapping("/cart")
    public String cartPage() {
        return "redirect:/cart.html";
    }

    @GetMapping("/checkout")
    public String checkoutPage() {
        return "redirect:/checkout.html";
    }

    @GetMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<CartDTO> getCart(HttpSession session) {
        Long cartId = getOrCreateCartId(session);
        return cartClientService.getCart(cartId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cart/add")
    public String addItem(
            @RequestParam String productId,
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "0") double price,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {
        Long cartId = getOrCreateCartId(session);
        cartClientService.addItem(cartId, productId, productName != null ? productName : "", price, quantity);
        return "redirect:/cart.html";
    }

    @PostMapping("/cart/remove")
    public String removeItem(
            @RequestParam Long itemId,
            HttpSession session) {
        Long cartId = getOrCreateCartId(session);
        cartClientService.removeItem(cartId, itemId);
        return "redirect:/cart.html";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(
            @RequestParam Long itemId,
            @RequestParam int quantity,
            HttpSession session) {
        Long cartId = getOrCreateCartId(session);
        if (quantity < 1) {
            cartClientService.removeItem(cartId, itemId);
        } else {
            cartClientService.updateItem(cartId, itemId, quantity);
        }
        return "redirect:/cart.html";
    }

    private Long getOrCreateCartId(HttpSession session) {
        Long cartId = (Long) session.getAttribute(SESSION_CART_ID);
        if (cartId == null) {
            cartId = cartClientService.createCart().orElseThrow(() -> new RuntimeException("Failed to create cart"));
            session.setAttribute(SESSION_CART_ID, cartId);
        }
        return cartId;
    }
}
```

**What it does:** Stores the current cart id in the session under `"cartId"`. `getOrCreateCartId` creates a cart on first use. Routes: `/cart` and `/checkout` redirect to the static pages; `/api/cart` returns the current cart as JSON; `/cart/add`, `/cart/remove`, and `/cart/update` perform the action and redirect back to the cart page.

---

## Part 4: Web Site UI Update

### Download the revised static files here

static10-1a.zip

---

## Part 5: Run and test

1. Start the **db** module (e.g. on port 8083).
2. Start the **api-mongo** module (e.g. on port 8082).
3. Start the **web** module (e.g. on port 8080). Ensure `db.service.base-url` in `application.properties` matches the db module’s URL and port.
4. In the browser: open a product, click Add to Cart, and confirm you are redirected to the cart and the item appears. On the cart page, change quantity (Update) and remove an item; then click Proceed to Checkout and confirm the placeholder page loads.

---

## Summary

After following this tutorial you will have:

- **db module:** REST API at `/api/cart` with CartDTO/CartItemDTO, String productId, clearCart, and a controller that returns only DTOs.
- **web module:** `db.service.base-url`, WebClient bean `dbServiceClient`, CartClientService, and CartController with session-based cart id and routes for `/cart`, `/checkout`, `/api/cart`, `/cart/add`, `/cart/remove`, `/cart/update`.
- **ui:** Updated user interface that includes cart functionality. 