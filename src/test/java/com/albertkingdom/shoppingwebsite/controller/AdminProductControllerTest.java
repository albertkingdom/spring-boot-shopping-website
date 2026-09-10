package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.response.UploadedImage;
import com.albertkingdom.shoppingwebsite.exception.ConflictException;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.service.CloudinaryService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ProductService productService;
    @MockBean
    private CloudinaryService cloudinaryService;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void createPlatformProduct_deletesUploadedImage_whenDatabaseWriteFails() throws Exception {
        MockMultipartFile image = new MockMultipartFile("productImage", "product.jpg", MediaType.IMAGE_JPEG_VALUE,
                "image bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(cloudinaryService.uploadImage(any())).thenReturn(new UploadedImage("https://image", "new-image"));
        when(productService.createPlatformProduct(ArgumentMatchers.any(Product.class), eq("admin@example.com")))
                .thenThrow(new ConflictException("database write failed"));

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("productName", "Platform product")
                        .param("productPrice", "10.00")
                        .principal(() -> "admin@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("database write failed"));

        verify(cloudinaryService).deleteFile("new-image");
    }

    @Test
    void createPlatformProduct_keepsOriginalDatabaseError_whenCompensationDeleteThrowsRuntimeException() throws Exception {
        MockMultipartFile image = new MockMultipartFile("productImage", "product.jpg", MediaType.IMAGE_JPEG_VALUE,
                "image bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(cloudinaryService.uploadImage(any())).thenReturn(new UploadedImage("https://image", "new-image"));
        when(productService.createPlatformProduct(ArgumentMatchers.any(Product.class), eq("admin@example.com")))
                .thenThrow(new ConflictException("database write failed"));
        org.mockito.Mockito.doThrow(new IllegalStateException("Cloudinary client failed"))
                .when(cloudinaryService).deleteFile("new-image");

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("productName", "Platform product")
                        .param("productPrice", "10.00")
                        .principal(() -> "admin@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("database write failed"));

        verify(cloudinaryService).deleteFile("new-image");
    }

    @Test
    void createPlatformProduct_keepsOriginalDatabaseError_whenCompensationDeleteThrowsIOException() throws Exception {
        MockMultipartFile image = new MockMultipartFile("productImage", "product.jpg", MediaType.IMAGE_JPEG_VALUE,
                "image bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(cloudinaryService.uploadImage(any())).thenReturn(new UploadedImage("https://image", "new-image"));
        when(productService.createPlatformProduct(ArgumentMatchers.any(Product.class), eq("admin@example.com")))
                .thenThrow(new ConflictException("database write failed"));
        org.mockito.Mockito.doThrow(new java.io.IOException("Cloudinary unavailable"))
                .when(cloudinaryService).deleteFile("new-image");

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("productName", "Platform product")
                        .param("productPrice", "10.00")
                        .principal(() -> "admin@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("database write failed"));

        verify(cloudinaryService).deleteFile("new-image");
    }
}
