package com.albertkingdom.shoppingwebsite.dto.response;

import com.albertkingdom.shoppingwebsite.model.Order;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Compact order representation for listing endpoints. Deliberately omits
 * line items — callers that need those should hit /api/order/{id}.
 */
public class OrderSummaryResponse {

    private final Long id;
    private final Timestamp createdAt;
    private final BigDecimal priceSum;
    private final Long userId;

    public OrderSummaryResponse(Long id, Timestamp createdAt, BigDecimal priceSum, Long userId) {
        this.id = id;
        this.createdAt = createdAt;
        this.priceSum = priceSum;
        this.userId = userId;
    }

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getCreatedAt(), order.getPriceSum(), order.getUserId());
    }

    public Long getId() {
        return id;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getPriceSum() {
        return priceSum;
    }

    public Long getUserId() {
        return userId;
    }
}
