package com.albertkingdom.shoppingwebsite.model;

/*
for order request structure
 */

import java.util.List;

public class OrderRequest {

    private List<OrderRequestItem> items;
    private Long userId;
    private Float totalPrice;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Float getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Float totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<OrderRequestItem> getItems() {
        return items;
    }

    public void setItems(List<OrderRequestItem> items) {
        this.items = items;
    }

}

