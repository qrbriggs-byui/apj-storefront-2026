package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name="orderx")
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private String customerName;

    // When you save the parent entity for the first time, JPA will also automatically save any new related entities.
    // When you update an existing entity, changes to related entities will also be updated automatically.
    // CascadeType.ALL includes REMOVE, which could delete shared data accidentally. We don't use this here.
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "product_order",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "order_id")
    )
    private List<Product> products;

    @OneToOne(mappedBy = "order")
    private Payment payment;

}
