package com.albertkingdom.shoppingwebsite.dto.response;

import com.albertkingdom.shoppingwebsite.model.OrderItem;

import java.math.BigDecimal;

/**
 * One line item on an order detail response. Reads the snapshot fields
 * stored on {@code OrderItem}, not the current product row — see
 * docs/order-immutability.md for why.
 */
public class OrderItemResponse {

    private final String productName;
    private final BigDecimal unitPrice;
    private final Integer quantity;

    public OrderItemResponse(String productName, BigDecimal unitPrice, Integer quantity) {
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProductName(), item.getUnitPrice(), item.getQuantity());
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
