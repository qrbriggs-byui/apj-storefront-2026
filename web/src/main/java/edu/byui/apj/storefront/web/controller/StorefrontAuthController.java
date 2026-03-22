package edu.byui.apj.storefront.web.controller;

import edu.byui.apj.storefront.web.model.LoginApiRequest;
import edu.byui.apj.storefront.web.model.RegisterApiRequest;
import edu.byui.apj.storefront.web.service.AccountClientService;
import edu.byui.apj.storefront.web.service.SecurityLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Form POST endpoints for login and registration. On success, delegates to {@link SecurityLoginService}
 * to establish ROLE_USER in the Spring Security context, then redirects to the storefront.
 */
@Controller
public class StorefrontAuthController {

    private final AccountClientService accountClientService;
    private final SecurityLoginService securityLoginService;

    public StorefrontAuthController(AccountClientService accountClientService,
                                    SecurityLoginService securityLoginService) {
        this.accountClientService = accountClientService;
        this.securityLoginService = securityLoginService;
    }

    @PostMapping("/auth/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        return accountClientService.login(new LoginApiRequest(username, password))
                .map(account -> {
                    securityLoginService.establishAuthenticatedSession(account, request, response);
                    return "redirect:/profile.html";
                })
                .orElse("redirect:/login.html?error=1");
    }

    @PostMapping("/auth/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String shippingZip,
            HttpServletRequest request,
            HttpServletResponse response) {
        return accountClientService.register(new RegisterApiRequest(
                        username, password, firstName, lastName, shippingZip))
                .map(account -> {
                    securityLoginService.establishAuthenticatedSession(account, request, response);
                    return "redirect:/profile.html";
                })
                .orElse("redirect:/register.html?error=duplicate");
    }
}
