package edu.byui.apj.storefront.work.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cart.cleanup")
public class CartCleanupProperties {

    private int maxAgeMinutes = 30;
    private long fixedDelayMs = 60000;

    public int getMaxAgeMinutes() {
        return maxAgeMinutes;
    }

    public void setMaxAgeMinutes(int maxAgeMinutes) {
        this.maxAgeMinutes = maxAgeMinutes;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }
}
