package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue
    Long id;

    String name;
    String description;
    Double price;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "product")
    List<PriceChange> priceChanges;

    @ManyToMany(mappedBy = "products")
    List<Order> orders;
}
