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
