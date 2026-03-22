package edu.byui.apj.storefront.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Excludes {@link UserDetailsServiceAutoConfiguration} so Spring Boot does not create a default
 * in-memory user — authentication is established manually after the db module validates credentials.
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

}
