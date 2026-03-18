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
