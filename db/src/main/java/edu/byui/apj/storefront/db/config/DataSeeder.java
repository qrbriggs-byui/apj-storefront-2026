package edu.byui.apj.storefront.db.config;

import edu.byui.apj.storefront.db.model.*;
import edu.byui.apj.storefront.db.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo products and one customer account. Demo password is {@code password}
 * (BCrypt-hashed below — never store plaintext in the database).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ProductRepository productRepository,
                      CategoryRepository categoryRepository,
                      UserProfileRepository userProfileRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0 || productRepository.count() > 0) {
            log.info("Demo data already present, skipping seeding.");
            return;
        }

        // OneToOne: User ↔ UserProfile (persist profile first, then attach user with BCrypt hash)
        UserProfile p1 = new UserProfile();
        p1.setFirstName("Jane");
        p1.setLastName("Doe");
        p1.setShippingZip("83440");
        p1 = userProfileRepository.save(p1);

        User u1 = new User();
        u1.setUsername("jane.doe");
        u1.setPasswordHash(passwordEncoder.encode("password"));
        u1.setProfile(p1);
        u1 = userRepository.save(u1);
        p1.setUser(u1);
        userProfileRepository.save(p1);

        // ManyToMany: Product ↔ Category
        Category electronics = new Category(); electronics.setName("Electronics");
        Category mobile = new Category(); mobile.setName("Mobile");

        Product phone = new Product(); phone.setName("Phone X");
        phone.getCategories().add(electronics);
        phone.getCategories().add(mobile);

        Product tablet = new Product(); tablet.setName("Tablet Pro");
        tablet.getCategories().add(electronics);

        productRepository.save(phone);
        productRepository.save(tablet);

        log.info("Seeding complete. Users: {}, Products: {}, Categories: {}",
                userRepository.count(), productRepository.count(), categoryRepository.count());
    }
}
