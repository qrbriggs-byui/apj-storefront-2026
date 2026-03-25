package edu.byui.apj.storefront.web.config;

import edu.byui.apj.storefront.web.security.DbJwtAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security: form login, in-memory users with roles, JWT handoff to the db service after login,
 * and role-based access to profile and cart routes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails shopper = User.builder()
                .username("shopper")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        UserDetails manager = User.builder()
                .username("manager")
                .password(passwordEncoder.encode("admin"))
                .roles("USER", "ADMIN")
                .build();
        return new InMemoryUserDetailsManager(shopper, manager);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DbJwtAuthenticationSuccessHandler loginSuccessHandler)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me/session").permitAll()
                        .requestMatchers("/profile.html", "/api/me/profile").hasRole("USER")
                        .requestMatchers(
                                "/api/cart",
                                "/cart",
                                "/cart/**",
                                "/checkout",
                                "/checkout/**",
                                "/cart.html",
                                "/checkout.html",
                                "/order-confirmation.html",
                                "/order-confirmation/**",
                                "/order-status/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .logoutSuccessUrl("/index.html")
                        .permitAll()
                );
        return http.build();
    }
}
