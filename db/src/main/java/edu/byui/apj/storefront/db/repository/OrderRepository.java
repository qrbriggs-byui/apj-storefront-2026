package edu.byui.apj.storefront.db.repository;

import edu.byui.apj.storefront.db.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
