package edu.byui.apj.storefront.web.service;

import edu.byui.apj.storefront.web.model.StorefrontUserPrincipal;
import edu.byui.apj.storefront.web.model.UserAccountResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * After the db module validates credentials, this stores an authenticated {@link StorefrontUserPrincipal}
 * with {@code ROLE_USER} in the security context (Article 13–style authorization).
 * <p>
 * The context must be written to the HTTP session explicitly; setting {@link SecurityContextHolder} alone
 * is not reliably persisted before a redirect, so the next request would still be anonymous.
 */
@Service
public class SecurityLoginService {

    private static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * Marks the current session as logged in with the given account (no password kept in memory).
     */
    public void establishAuthenticatedSession(UserAccountResponse account,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        StorefrontUserPrincipal principal = new StorefrontUserPrincipal(account.id(), account.username());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(ROLE_USER));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
