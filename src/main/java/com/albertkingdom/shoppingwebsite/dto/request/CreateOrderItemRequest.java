package com.albertkingdom.shoppingwebsite.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateOrderItemRequest {

    @NotNull(message = "productId is required.")
    private Long productId;

    @NotNull(message = "quantity is required.")
    @Positive(message = "quantity must be greater than zero.")
    private Integer quantity;

    public CreateOrderItemRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
