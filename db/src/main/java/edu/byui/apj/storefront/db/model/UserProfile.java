package edu.byui.apj.storefront.db.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    /** US-style ZIP: 5 digits or 5+4 (e.g. 83440 or 83440-1234). */
    @Column(name = "shipping_zip", length = 16)
    private String shippingZip;

    @OneToOne(mappedBy = "profile")
    private User user;
}