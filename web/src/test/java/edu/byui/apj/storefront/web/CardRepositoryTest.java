package edu.byui.apj.storefront.web;

import edu.byui.apj.storefront.web.model.Card;
import edu.byui.apj.storefront.web.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Updated test that checks the CSV-backed featured search returns at least one
 * Card with "java" in name, specialty, or contribution (case-insensitive).
 *
 * This replaces the previous test that referenced `description`. See original:
 * :contentReference[oaicite:1]{index=1}
 */
@SpringBootTest
class CardRepositoryTest {

    @Autowired
    CardService service;

    @Test
    void featured_hasJava() {
        List<Card> featured = service.getFeaturedCards(null);
        assertNotNull(featured, "Featured list should not be null");

        boolean anyJava = featured.stream().anyMatch(c ->
                containsIgnoreCase(c.getName(), "java")
                        || containsIgnoreCase(c.getSpecialty(), "java")
                        || containsIgnoreCase(c.getContribution(), "java")
        );

        assertTrue(anyJava, "No card contains 'Java' in name, specialty, or contribution — check CSV and search logic");
    }

    // Helper for null-safe, case-insensitive contains check
    private static boolean containsIgnoreCase(String source, String sub) {
        if (source == null || sub == null) return false;
        return source.toLowerCase().contains(sub.toLowerCase());
    }
}