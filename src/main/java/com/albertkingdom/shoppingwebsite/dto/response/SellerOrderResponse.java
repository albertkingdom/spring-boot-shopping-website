package com.albertkingdom.shoppingwebsite.dto.response;

import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.OrderItem;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

public class SellerOrderResponse {
    private final Long id;
    private final Timestamp createdAt;
    private final String buyerEmail;
    private final List<OrderItemResponse> items;
    private final BigDecimal sellerSubtotal;

    public SellerOrderResponse(Long id, Timestamp createdAt, String buyerEmail,
                               List<OrderItemResponse> items, BigDecimal sellerSubtotal) {
        this.id = id;
        this.createdAt = createdAt;
        this.buyerEmail = buyerEmail;
        this.items = items;
        this.sellerSubtotal = sellerSubtotal;
    }

    public static SellerOrderResponse from(Order order, String buyerEmail, Long sellerId) {
        List<OrderItem> sellerItems = order.getOrderItems().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .collect(Collectors.toList());
        BigDecimal subtotal = sellerItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SellerOrderResponse(order.getId(), order.getCreatedAt(), buyerEmail,
                sellerItems.stream().map(OrderItemResponse::from).collect(Collectors.toList()), subtotal);
    }

    public Long getId() { return id; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getBuyerEmail() { return buyerEmail; }
    public List<OrderItemResponse> getItems() { return items; }
    public BigDecimal getSellerSubtotal() { return sellerSubtotal; }
}
