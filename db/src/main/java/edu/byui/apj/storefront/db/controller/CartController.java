package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.model.Cart;
import edu.byui.apj.storefront.db.model.Item;
import edu.byui.apj.storefront.db.service.CartService;
import edu.byui.apj.storefront.db.controller.dto.AddItemRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/carts")  // recommended plural resource
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Cart> create(UriComponentsBuilder uriBuilder) {
        Cart cart = service.createCart();
        var location = uriBuilder.path("/carts/{id}").buildAndExpand(cart.getId()).toUri();
        return ResponseEntity.created(location).body(cart);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cart> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getCart(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Item> addItem(@PathVariable("id") Long cartId,
                                        @RequestBody AddItemRequest req,
                                        UriComponentsBuilder uriBuilder) {
        Item created = service.addItemToCart(cartId, req.productId(), req.name(), req.price(), req.quantity());
        var location = uriBuilder.path("/carts/{cartId}/items/{itemId}")
                .buildAndExpand(cartId, created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Item> updateItem(@PathVariable Long cartId,
                                           @PathVariable Long itemId,
                                           @RequestBody AddItemRequest req) {
        Item updated = service.updateItem(cartId, itemId, req.quantity());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long cartId, @PathVariable Long itemId) {
        service.removeItem(cartId, itemId);
        return ResponseEntity.noContent().build();
    }
}