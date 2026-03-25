package edu.byui.apj.storefront.db.controller;

import edu.byui.apj.storefront.db.controller.dto.LoginRequest;
import edu.byui.apj.storefront.db.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthController} using Mockito (Article 15-1): we fake
 * {@link AuthenticationManager} and {@link JwtService} so we can verify how the controller uses
 * them without starting Spring or hitting a database.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    /** Mockito fills this constructor with the {@code @Mock} fields above. */
    @InjectMocks
    private AuthController authController;

    @Test
    void login_whenUsernameMissing_returnsBadRequestAndDoesNotCallDependencies() {
        // Guard clause path: bad input → 400, and we should not hit security or JWT code
        var response = authController.login(new LoginRequest(null, "password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenPasswordMissing_returnsBadRequest() {
        var response = authController.login(new LoginRequest("shopper", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_whenAuthenticationFails_returns401() {
        // Spring Security signals bad password with AuthenticationException (here: BadCredentialsException)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        var response = authController.login(new LoginRequest("shopper", "wrong"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Must not issue a JWT if login failed
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_whenAuthenticationSucceeds_returnsTokenAndVerifiesCollaborations() {
        // authenticate(...) returns a filled Authentication when username/password match
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtService.generateToken("shopper")).thenReturn("signed-jwt");

        var response = authController.login(new LoginRequest("shopper", "password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("signed-jwt");

        // verify() = behavioral assertion: did we delegate to collaborators correctly?
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(eq("shopper"));
    }
}
