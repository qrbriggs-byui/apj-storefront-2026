package edu.byui.apj.storefront.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.byui.apj.storefront.db.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {}