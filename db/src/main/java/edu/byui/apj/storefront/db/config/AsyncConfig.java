package edu.byui.apj.storefront.db.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async method execution. Async methods run in a separate thread pool
 * so the calling thread returns immediately (e.g. HTTP response) while work continues in the background.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
