package com.albertkingdom.shoppingwebsite.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full order view for GET /api/order/{id}. Composes a per-order summary
 * with the line-item snapshots.
 */
public class OrderDetailResponse {

    private final Long id;
    private final BigDecimal priceSum;
    private final Long userId;
    private final String userEmail;
    private final List<OrderItemResponse> items;

    public OrderDetailResponse(Long id, BigDecimal priceSum, Long userId, String userEmail, List<OrderItemResponse> items) {
        this.id = id;
        this.priceSum = priceSum;
        this.userId = userId;
        this.userEmail = userEmail;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getPriceSum() {
        return priceSum;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
}
