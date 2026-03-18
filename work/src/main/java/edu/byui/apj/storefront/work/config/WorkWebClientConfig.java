package edu.byui.apj.storefront.work.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WorkWebClientConfig {

    @Value("${db.service.base-url}")
    private String dbServiceBaseUrl;

    @Bean
    public WebClient dbServiceClient() {
        return WebClient.builder()
                .baseUrl(dbServiceBaseUrl)
                .build();
    }
}
