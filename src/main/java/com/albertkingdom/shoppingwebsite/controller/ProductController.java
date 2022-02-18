package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.sevice.ProductService;
import com.albertkingdom.shoppingwebsite.sevice.ProductServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private ProductServiceImpl productServiceImpl;

    public ProductController(ProductServiceImpl productServiceImpl) {
        this.productServiceImpl = productServiceImpl;
    }
    @PostMapping
    public ResponseEntity<Product> saveProduct(@RequestBody Product product) {
        Product newProduct = productServiceImpl.saveProduct(product);
        //System.out.println("post" + newProduct);
        return ResponseEntity.ok().body(newProduct);
    }
    @GetMapping
    public List<Product> getAllProducts(HttpSession session) {
        return productServiceImpl.getAllProducts();
    }

    // http://localhost:8080/api/products/1
    @GetMapping("{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") Long id) {
        Product product = productServiceImpl.getProductById(id);
        //System.out.println("get by id" + product);

        return new ResponseEntity<Product>(product, HttpStatus.OK);
    }

    @PutMapping("{id}")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product, @PathVariable("id") Long id) {
        Product updatedProduct = productServiceImpl.updateProduct(product, id);
        //System.out.println("put " + updatedProduct.getName());

        return new ResponseEntity<Product>(updatedProduct, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable("id") Long id) {
        productServiceImpl.deleteProduct(id);
        return new ResponseEntity<String>("Product deleted successfully", HttpStatus.OK);
    }
}
