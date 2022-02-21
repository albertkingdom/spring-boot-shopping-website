package com.albertkingdom.shoppingwebsite.model;

import java.util.ArrayList;
import java.util.List;


public class CustomOrderResponse {
    private Long id;
    private Float priceSum;
    private Long userId;
    private String userEmail;
    private List<OrderItemDetail> orderItemDetailList;


    public CustomOrderResponse(Long id, Float priceSum, Long userId, String userEmail, List<OrderItemDetail> orderItemDetailList) {
        this.id = id;
        this.priceSum = priceSum;
        this.userId = userId;
        this.userEmail = userEmail;
        this.orderItemDetailList = orderItemDetailList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getPriceSum() {
        return priceSum;
    }

    public void setPriceSum(Float priceSum) {
        this.priceSum = priceSum;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    public List<OrderItemDetail> getOrderItemDetailList() {
        return orderItemDetailList;
    }

    public void setOrderItemDetailList(List<OrderItemDetail> orderItemDetailList) {
        this.orderItemDetailList = orderItemDetailList;
    }
}
