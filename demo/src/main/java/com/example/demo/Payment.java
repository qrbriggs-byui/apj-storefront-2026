package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Payment {

    @Id
    private Long id;

    private String transactionIdentifier;

    @Column(name="foo")
    private String paymentType;

    @OneToOne(cascade = CascadeType.ALL)
    private Order order;

}
