package edu.byui.apj.storefront.db.repository;

import edu.byui.apj.storefront.db.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
