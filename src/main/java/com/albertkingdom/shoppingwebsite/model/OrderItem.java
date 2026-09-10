package com.albertkingdom.shoppingwebsite.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import java.math.BigDecimal;

/*
for order request item {productId: xx, quantity: yy}
 */
@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "order_id")
    private Order order;


    private Long productId;

    private Integer quantity;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;


    public OrderItem() {
    }

    public OrderItem(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    /**
     * Snapshot the product's current name and unit price onto this line item so
     * a later rename, reprice, or deletion cannot change historical orders.
     */
    public static OrderItem snapshotOf(Product product, Integer quantity) {
        OrderItem item = new OrderItem(product.getId(), quantity);
        item.productName = product.getName();
        item.unitPrice = product.getPrice();
        // A null seller represents a platform-managed legacy product. Such a
        // line stays visible to platform admins but is excluded from every
        // seller-scoped order view.
        item.sellerId = product.getSeller() == null ? null : product.getSeller().getId();
        return item;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
