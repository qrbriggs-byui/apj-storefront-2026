package edu.byui.apj.storefront.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides a WebClient configured to call the Cards API module.
 * Article 6 shows WebClient as the recommended non-blocking HTTP client.
 */
@Configuration
public class WebClientConfig {

    @Value("${card.api.base-url:http://localhost:8081}")
    private String cardApiBaseUrl;

    @Bean
    public WebClient cardApiClient() {
        return WebClient.builder()
                .baseUrl(cardApiBaseUrl)
                .build();
    }
}