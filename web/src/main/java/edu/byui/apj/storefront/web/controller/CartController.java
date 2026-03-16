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
