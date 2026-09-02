package com.albertkingdom.shoppingwebsite.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Public shape for POST /api/order. Only a list of line items is accepted;
 * the total price is always recomputed on the server from current product
 * prices so a client cannot dictate what the order costs, and userId is
 * always resolved from the authenticated principal, never from the body.
 */
public class CreateOrderRequest {

    @NotEmpty(message = "items must contain at least one entry.")
    @Valid
    private List<CreateOrderItemRequest> items;

    public CreateOrderRequest() {
    }

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }
}
