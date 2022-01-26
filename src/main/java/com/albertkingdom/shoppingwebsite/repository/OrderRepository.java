package com.albertkingdom.shoppingwebsite.repository;

import com.albertkingdom.shoppingwebsite.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
