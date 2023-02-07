package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.model.ProductsPagination;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);
    List<Product> getAllProducts();

    ProductsPagination getProductsByPage(int page);
    Product getProductById(Long id);
    Product updateProduct(Product product, Long id);
    void deleteProduct(Long id);
}
