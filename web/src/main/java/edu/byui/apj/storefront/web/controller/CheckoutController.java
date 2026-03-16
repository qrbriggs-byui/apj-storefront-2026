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
