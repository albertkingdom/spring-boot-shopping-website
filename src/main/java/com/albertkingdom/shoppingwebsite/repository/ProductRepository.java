package com.albertkingdom.shoppingwebsite.repository;

import com.albertkingdom.shoppingwebsite.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
    boolean existsBySellerId(Long sellerId);
}
