package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.UserProfileDto;
import edu.byui.apj.storefront.web.service.InMemoryUserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated profile API (browser session). Username comes from Spring Security, not the client.
 */
@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final InMemoryUserProfileService userProfileService;

    public ProfileController(InMemoryUserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> profile(@AuthenticationPrincipal UserDetails user) {
        return userProfileService.findByUsername(user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
