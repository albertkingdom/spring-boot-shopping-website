package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.dto.response.UploadedImage;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.service.CloudinaryService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/admin/products")
@Validated
public class AdminProductController {
    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    public AdminProductController(ProductService productService, CloudinaryService cloudinaryService) {
        this.productService = productService;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createPlatformProduct(
            @RequestParam("productName") @NotBlank String productName,
            @RequestParam("productPrice") @NotBlank @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$") String productPrice,
            @RequestParam(value = "productImage", required = false) MultipartFile file,
            Principal principal) {
        UploadedImage uploadedImage = null;
        try {
            productService.verifyPlatformProductCreationAccess(principal.getName());
            if (file != null && !file.isEmpty()) {
                uploadedImage = cloudinaryService.uploadImage(file);
            }
            Product product = productService.createPlatformProduct(
                    new Product(productName, new BigDecimal(productPrice), imageUrl(uploadedImage), imageName(uploadedImage)),
                    principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            compensateUploadedImage(uploadedImage);
            throw e;
        }
    }

    private String imageUrl(UploadedImage uploadedImage) {
        return uploadedImage == null ? null : uploadedImage.getUrl();
    }

    private String imageName(UploadedImage uploadedImage) {
        return uploadedImage == null ? null : uploadedImage.getPublicId();
    }

    private void compensateUploadedImage(UploadedImage uploadedImage) {
        if (uploadedImage == null) {
            return;
        }
        try {
            cloudinaryService.deleteFile(uploadedImage.getPublicId());
        } catch (Exception compensationFailure) {
            log.warn("cloudinary compensation delete failed operation=create-platform-product publicId={} exception={}",
                    uploadedImage.getPublicId(), compensationFailure.getClass().getSimpleName());
        }
    }
}
