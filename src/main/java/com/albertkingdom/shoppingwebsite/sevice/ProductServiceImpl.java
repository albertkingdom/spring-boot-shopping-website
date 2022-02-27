package com.albertkingdom.shoppingwebsite.sevice;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService{
    private ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

//    create a product in db
    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

//    list all products
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @Override
    public Product updateProduct(Product product, Long id) {
        Product existedProduct = getProductById(id);
        existedProduct.setPrice(product.getPrice());
        existedProduct.setName(product.getName());
        existedProduct.setImgName(product.getImgName());
        existedProduct.setImgUrl(product.getImgUrl());
        return saveProduct(existedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.findById(id).orElseThrow();
        productRepository.deleteById(id);
    }


}
