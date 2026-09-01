package com.albertkingdom.shoppingwebsite.model;

import java.math.BigDecimal;

public class OrderItemDetail {
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;

    public OrderItemDetail(String productName, BigDecimal productPrice, Integer quantity) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
