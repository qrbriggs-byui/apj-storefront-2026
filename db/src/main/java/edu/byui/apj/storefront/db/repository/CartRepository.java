package edu.byui.apj.storefront.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.byui.apj.storefront.db.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {}