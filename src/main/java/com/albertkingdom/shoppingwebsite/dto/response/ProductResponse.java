package com.albertkingdom.shoppingwebsite.dto.response;

import com.albertkingdom.shoppingwebsite.model.Product;

import java.math.BigDecimal;

/**
 * Public shape for product read endpoints. Excludes {@code imgName}
 * (Cloudinary public_id, an internal storage detail) so the API surface
 * does not leak persistence identifiers.
 */
public class ProductResponse {

    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final String imgUrl;

    public ProductResponse(Long id, String name, BigDecimal price, String imgUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imgUrl = imgUrl;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getImgUrl());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImgUrl() {
        return imgUrl;
    }
}
