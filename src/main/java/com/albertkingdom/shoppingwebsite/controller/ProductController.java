package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.dto.response.UploadedImage;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.service.CloudinaryService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    public ProductController(ProductService productService, CloudinaryService cloudinaryService) {
        this.productService = productService;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> saveProduct(
            @RequestParam("productName") @NotBlank String productName,
            @RequestParam("productPrice") @NotBlank @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Must be a non-negative decimal with up to 2 fractional digits.") String productPrice,
            @RequestParam(value = "productImage", required = false) MultipartFile file
    ) {

        String imgUrl = null;
        String imgName = null;
        try {
            if (file != null && !file.isEmpty()) {
                UploadedImage uploaded = cloudinaryService.uploadImage(file);
                imgUrl = uploaded.getUrl();
                imgName = uploaded.getPublicId();
            }
            Product newProduct = productService.saveProduct(
                    new Product(productName, new BigDecimal(productPrice), imgUrl, imgName));
            return ResponseEntity.ok().body(ProductResponse.from(newProduct));
        } catch (IOException e) {
            log.error("failed to save product name={}", productName, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping
    public PageResponse<ProductResponse> getProductsByPage(
            @RequestParam(name = "page", defaultValue = "0") @Min(value = 0, message = "page must be zero or greater.") int page) {
        return productService.getProductsByPage(page);
    }

    // http://localhost:8080/api/products/1
    @GetMapping("{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") Long id) {
        Product product = productService.getProductById(id);
        return new ResponseEntity<>(ProductResponse.from(product), HttpStatus.OK);
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @RequestParam("productName") @NotBlank String productName,
            @RequestParam("productPrice") @NotBlank @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Must be a non-negative decimal with up to 2 fractional digits.") String productPrice,
            @RequestParam(value = "productImage", required = false) MultipartFile file,
            @PathVariable("id") Long id
    ) {

        String imgUrl = null;
        String imgName = null;
        try {
            if (file != null && !file.isEmpty()) {
                UploadedImage uploaded = cloudinaryService.uploadImage(file);
                imgUrl = uploaded.getUrl();
                imgName = uploaded.getPublicId();
            }
            // Nulls signal "no change" — updateProduct preserves the existing image
            // when the caller didn't attach a new one.
            Product updatedProduct = productService.updateProduct(
                    new Product(productName, new BigDecimal(productPrice), imgUrl, imgName), id);
            return ResponseEntity.ok().body(ProductResponse.from(updatedProduct));
        } catch (IOException e) {
            log.error("failed to update product id={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable("id") Long id) throws IOException {
        Product existedProduct = productService.getProductById(id);
        String imgName = existedProduct.getImgName();
        productService.deleteProduct(id);
        cloudinaryService.deleteFile(imgName);
        return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
    }
}
