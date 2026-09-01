package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public PageResponse<ProductResponse> getProductsByPage(int page) {
        Pageable pageWithTenElementsDesc = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<Product> result = productRepository.findAll(pageWithTenElementsDesc);
        return PageResponse.of(result, ProductResponse::from);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    @Transactional
    public Product updateProduct(Product product, Long id) {
        Product existedProduct = getProductById(id);
        existedProduct.setPrice(product.getPrice());
        existedProduct.setName(product.getName());
        existedProduct.setImgName(product.getImgName());
        existedProduct.setImgUrl(product.getImgUrl());
        return saveProduct(existedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.findById(id).orElseThrow(RuntimeException::new);
        productRepository.deleteById(id);
    }
}
