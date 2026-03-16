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