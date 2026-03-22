package edu.byui.apj.storefront.db.service;

import edu.byui.apj.storefront.db.controller.dto.LoginRequest;
import edu.byui.apj.storefront.db.controller.dto.RegisterRequest;
import edu.byui.apj.storefront.db.controller.dto.UserAccountResponse;
import edu.byui.apj.storefront.db.model.User;
import edu.byui.apj.storefront.db.model.UserProfile;
import edu.byui.apj.storefront.db.repository.UserProfileRepository;
import edu.byui.apj.storefront.db.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

/**
 * Registration, login, and profile read. Passwords are hashed with BCrypt before storage.
 * Validation is kept in service methods to limit tutorial complexity.
 */
@Service
public class UserAccountService {

    private static final Pattern US_ZIP = Pattern.compile("^\\d{5}(-\\d{4})?$");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository,
                            UserProfileRepository userProfileRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccountResponse register(RegisterRequest req) {
        String username = trimOrEmpty(req.username());
        String password = req.password() != null ? req.password() : "";
        String firstName = trimOrEmpty(req.firstName());
        String lastName = trimOrEmpty(req.lastName());
        String zip = trimOrEmpty(req.shippingZip());

        if (username.length() < 2 || username.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be 2–64 characters");
        }
        if (password.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 4 characters");
        }
        if (firstName.isEmpty() || lastName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First and last name are required");
        }
        if (!US_ZIP.matcher(zip).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP must be 5 digits or 5+4 format");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setShippingZip(zip);
        profile = userProfileRepository.save(profile);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setProfile(profile);
        user = userRepository.save(user);

        profile.setUser(user);
        userProfileRepository.save(profile);

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse login(LoginRequest req) {
        String username = trimOrEmpty(req.username());
        String password = req.password() != null ? req.password() : "";
        if (username.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static UserAccountResponse toResponse(User user) {
        UserProfile p = user.getProfile();
        return new UserAccountResponse(
                user.getId(),
                user.getUsername(),
                p != null ? p.getFirstName() : "",
                p != null ? p.getLastName() : "",
                p != null && p.getShippingZip() != null ? p.getShippingZip() : ""
        );
    }
}
