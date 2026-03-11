package com.example.demo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
@Data
public class Payment {

    @Id
    private Long id;

    private String transactionIdentifier;

    private String paymentType;

    @OneToOne(cascade = CascadeType.ALL)
    private Order order;

}
