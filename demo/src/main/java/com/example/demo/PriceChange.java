package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class PriceChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double newPrice;
    private Double oldPrice;
    private Date changeDate;

    @ManyToOne(cascade = CascadeType.ALL)
    private Product product;

}
