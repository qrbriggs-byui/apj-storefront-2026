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
