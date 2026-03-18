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