package com.albertkingdom.shoppingwebsite.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTest {

    @Test
    void snapshotOf_copiesProductNameAndUnitPrice() {
        Product product = new Product(42L, "T-Shirt", new BigDecimal("199.00"));

        OrderItem item = OrderItem.snapshotOf(product, 3);

        assertEquals(42L, item.getProductId());
        assertEquals(3, item.getQuantity());
        assertEquals("T-Shirt", item.getProductName());
        assertEquals(new BigDecimal("199.00"), item.getUnitPrice());
    }

    @Test
    void snapshotSurvivesLaterProductRenameAndReprice() {
        Product product = new Product(42L, "T-Shirt", new BigDecimal("199.00"));
        OrderItem item = OrderItem.snapshotOf(product, 1);

        // Simulate the product being renamed and repriced after the order was placed.
        product.setName("Renamed Shirt");
        product.setPrice(new BigDecimal("999.00"));

        assertEquals("T-Shirt", item.getProductName());
        assertEquals(new BigDecimal("199.00"), item.getUnitPrice());
    }
}
