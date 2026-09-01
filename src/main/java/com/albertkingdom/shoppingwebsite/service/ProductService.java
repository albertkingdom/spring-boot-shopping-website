package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.model.Product;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);
    List<Product> getAllProducts();

    PageResponse<ProductResponse> getProductsByPage(int page);
    Product getProductById(Long id);
    Product updateProduct(Product product, Long id);
    void deleteProduct(Long id);
}
