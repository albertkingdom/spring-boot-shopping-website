package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.model.Product;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);
    Product createProductForActor(Product product, String actorEmail);
    Product createPlatformProduct(Product product, String actorEmail);
    void verifyProductCreationAccess(String actorEmail);
    void verifyPlatformProductCreationAccess(String actorEmail);
    List<Product> getAllProducts();

    PageResponse<ProductResponse> getProductsByPage(int page);
    Product getProductById(Long id);
    Product updateProduct(Product product, Long id);
    void deleteProduct(Long id);
    void verifyProductManagementAccess(Long id, String actorEmail);
    Product updateProductForActor(Product product, Long id, String actorEmail);
    void deleteProductForActor(Long id, String actorEmail);
    PageResponse<ProductResponse> getProductsForSeller(String sellerEmail, int page);
}
