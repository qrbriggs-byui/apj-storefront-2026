package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.controller.dto.LoginRequest;
import edu.byui.apj.storefront.db.controller.dto.RegisterRequest;
import edu.byui.apj.storefront.db.controller.dto.UserAccountResponse;
import edu.byui.apj.storefront.db.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for registration, login, and profile read. The web module proxies these endpoints.
 */
@RestController
public class UserAccountApiController {

    private final UserAccountService userAccountService;

    public UserAccountApiController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<UserAccountResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userAccountService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<UserAccountResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userAccountService.login(request));
    }

    @GetMapping("/api/users/{id}/profile")
    public ResponseEntity<UserAccountResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userAccountService.getProfile(id));
    }
}
