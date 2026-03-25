package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.CartDTO;
import edu.byui.apj.storefront.web.security.WebSessionKeys;
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
        String jwt = requireDbJwt(session);
        Long cartId = getOrCreateCartId(session, jwt);
        return cartClientService.getCart(cartId, jwt)
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
        String jwt = requireDbJwt(session);
        Long cartId = getOrCreateCartId(session, jwt);
        cartClientService.addItem(cartId, jwt, productId, productName != null ? productName : "", price, quantity);
        return "redirect:/cart.html";
    }

    @PostMapping("/cart/remove")
    public String removeItem(
            @RequestParam Long itemId,
            HttpSession session) {
        String jwt = requireDbJwt(session);
        Long cartId = getOrCreateCartId(session, jwt);
        cartClientService.removeItem(cartId, itemId, jwt);
        return "redirect:/cart.html";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(
            @RequestParam Long itemId,
            @RequestParam int quantity,
            HttpSession session) {
        String jwt = requireDbJwt(session);
        Long cartId = getOrCreateCartId(session, jwt);
        if (quantity < 1) {
            cartClientService.removeItem(cartId, itemId, jwt);
        } else {
            cartClientService.updateItem(cartId, itemId, quantity, jwt);
        }
        return "redirect:/cart.html";
    }

    private static String requireDbJwt(HttpSession session) {
        String jwt = (String) session.getAttribute(WebSessionKeys.DB_JWT);
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalStateException("Missing JWT for db service; log in again.");
        }
        return jwt;
    }

    private Long getOrCreateCartId(HttpSession session, String jwt) {
        Long cartId = (Long) session.getAttribute(SESSION_CART_ID);
        if (cartId == null) {
            cartId = cartClientService.createCart(jwt).orElseThrow(() -> new RuntimeException("Failed to create cart"));
            session.setAttribute(SESSION_CART_ID, cartId);
        }
        return cartId;
    }
}
