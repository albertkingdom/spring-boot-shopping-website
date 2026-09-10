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
import java.security.Principal;

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
            @RequestParam(value = "productImage", required = false) MultipartFile file,
            Principal principal
    ) {

        UploadedImage uploadedImage = null;
        try {
            productService.verifyProductCreationAccess(principal.getName());
            if (file != null && !file.isEmpty()) {
                uploadedImage = cloudinaryService.uploadImage(file);
            }
            Product newProduct = productService.createProductForActor(
                    new Product(productName, new BigDecimal(productPrice), imageUrl(uploadedImage), imageName(uploadedImage)),
                    principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(newProduct));
        } catch (IOException e) {
            log.error("failed to save product name={}", productName, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            compensateUploadedImage(uploadedImage, "create-product");
            throw e;
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
            Principal principal,
            @PathVariable("id") Long id
    ) {

        UploadedImage uploadedImage = null;
        try {
            productService.verifyProductManagementAccess(id, principal.getName());
            if (file != null && !file.isEmpty()) {
                uploadedImage = cloudinaryService.uploadImage(file);
            }
            // Nulls signal "no change" — updateProduct preserves the existing image
            // when the caller didn't attach a new one.
            Product updatedProduct = productService.updateProductForActor(
                    new Product(productName, new BigDecimal(productPrice), imageUrl(uploadedImage), imageName(uploadedImage)),
                    id, principal.getName());
            return ResponseEntity.ok().body(ProductResponse.from(updatedProduct));
        } catch (IOException e) {
            log.error("failed to update product id={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            compensateUploadedImage(uploadedImage, "update-product");
            throw e;
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable("id") Long id, Principal principal) throws IOException {
        productService.verifyProductManagementAccess(id, principal.getName());
        Product existedProduct = productService.getProductById(id);
        String imgName = existedProduct.getImgName();
        productService.deleteProductForActor(id, principal.getName());
        cloudinaryService.deleteFile(imgName);
        return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
    }

    private String imageUrl(UploadedImage uploadedImage) {
        return uploadedImage == null ? null : uploadedImage.getUrl();
    }

    private String imageName(UploadedImage uploadedImage) {
        return uploadedImage == null ? null : uploadedImage.getPublicId();
    }

    private void compensateUploadedImage(UploadedImage uploadedImage, String operation) {
        if (uploadedImage == null) {
            return;
        }
        try {
            cloudinaryService.deleteFile(uploadedImage.getPublicId());
        } catch (Exception compensationFailure) {
            log.warn("cloudinary compensation delete failed operation={} publicId={} exception={}",
                    operation, uploadedImage.getPublicId(), compensationFailure.getClass().getSimpleName());
        }
    }
}
