package com.albertkingdom.shoppingwebsite.repository;

import com.albertkingdom.shoppingwebsite.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
