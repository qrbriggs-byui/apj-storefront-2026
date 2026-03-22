package edu.byui.apj.storefront.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Friendly URL that redirects to the static profile page. Access is enforced by {@code SecurityFilterChain}
 * ({@code /profile} requires ROLE_USER), so anonymous users never reach the redirect.
 */
@Controller
public class ProfilePageController {

    @GetMapping("/profile")
    public String profilePage() {
        return "redirect:/profile.html";
    }
}
