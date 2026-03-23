package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.UserProfileDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Read-only profile data keyed by username (matches {@link edu.byui.apj.storefront.web.config.SecurityConfig} users).
 */
@Service
public class InMemoryUserProfileService {

    private final Map<String, UserProfileDto> profilesByUsername = Map.of(
            "shopper", new UserProfileDto("Jane Shopper", "83701"),
            "manager", new UserProfileDto("Alex Manager", "83702")
    );

    public Optional<UserProfileDto> findByUsername(String username) {
        return Optional.ofNullable(profilesByUsername.get(username));
    }
}
