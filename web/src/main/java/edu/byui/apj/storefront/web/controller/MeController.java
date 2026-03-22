package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.StorefrontUserPrincipal;
import edu.byui.apj.storefront.web.model.UserAccountResponse;
import edu.byui.apj.storefront.web.service.AccountClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Browser JSON for viewing profile (read-only). User id comes from Spring Security, not the client.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountClientService accountClientService;

    public MeController(AccountClientService accountClientService) {
        this.accountClientService = accountClientService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserAccountResponse> getProfile(Authentication authentication) {
        StorefrontUserPrincipal p = (StorefrontUserPrincipal) authentication.getPrincipal();
        return accountClientService.getProfile(p.id())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
