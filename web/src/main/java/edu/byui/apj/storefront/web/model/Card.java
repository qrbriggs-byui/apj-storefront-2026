package edu.byui.apj.storefront.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Card model matching the CSV header exactly:
 * ID,Name,Specialty,Contribution,Price,ImageUrl
 *
 * We use Lombok to reduce boilerplate (@Data, @NoArgsConstructor, @AllArgsConstructor).
 *
 * Note: field names are conventional Java names and will be serialized to JSON
 * by Jackson when returned from controllers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private Long id;
    private String name;
    private String specialty;
    private String contribution;
    private BigDecimal price;
    private String imageUrl;
}