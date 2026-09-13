package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.SecurityConfig;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.filter.CustomAuthorizationFilter;
import com.albertkingdom.shoppingwebsite.handler.ApiExceptionHandler;
import com.albertkingdom.shoppingwebsite.service.OrderService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import com.albertkingdom.shoppingwebsite.service.UserServiceImpl;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({AdminSellerController.class, AdminProductController.class, ProductController.class, SellerProductController.class, SellerOrderController.class})
@Import({SecurityConfig.class, CustomAuthorizationFilter.class, ApiExceptionHandler.class})
class SellerAccessControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserServiceImpl userService;
    @MockBean
    private ProductService productService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private com.albertkingdom.shoppingwebsite.service.CloudinaryService cloudinaryService;

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void grantSellerRole_rejectsNormalUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/2/roles/seller"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerProducts_rejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/seller/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void grantSellerRole_allowsPlatformAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users/2/roles/seller"))
                .andExpect(status().isNoContent());

        verify(userService).grantSellerRole(2L, "admin@example.com");
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void sellerProducts_rejectsNormalUser() throws Exception {
        mockMvc.perform(get("/api/seller/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void sellerProducts_rejectsPlatformAdminWithoutSellerRole() throws Exception {
        mockMvc.perform(get("/api/seller/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "seller@example.com", authorities = "ROLE_SELLER")
    void sellerProducts_allowsSeller() throws Exception {
        when(productService.getProductsForSeller(eq("seller@example.com"), eq(0)))
                .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 0));

        mockMvc.perform(get("/api/seller/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "seller@example.com", authorities = "ROLE_SELLER")
    void sellerOrders_allowsSeller() throws Exception {
        when(orderService.getOrdersForSeller(eq("seller@example.com"), eq(0)))
                .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 0));

        mockMvc.perform(get("/api/seller/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void platformProductCreate_allowsPurePlatformAdmin() throws Exception {
        com.albertkingdom.shoppingwebsite.model.Product product =
                new com.albertkingdom.shoppingwebsite.model.Product(9L, "Platform item", new java.math.BigDecimal("10.00"));
        when(productService.createPlatformProduct(org.mockito.ArgumentMatchers.any(), eq("admin@example.com")))
                .thenReturn(product);

        mockMvc.perform(post("/api/admin/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Platform item")
                        .param("productPrice", "10.00"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void platformProductCreate_rejectsInvalidRequestParameters() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "")
                        .param("productPrice", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid parameter"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void platformProductCreate_rejectsNormalUser() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Platform item")
                        .param("productPrice", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void productCreate_rejectsAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Camera")
                        .param("productPrice", "10.00"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void productCreate_rejectsNormalUser() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Camera")
                        .param("productPrice", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "seller@example.com", authorities = "ROLE_SELLER")
    void productCreate_allowsSeller() throws Exception {
        when(productService.createProductForActor(org.mockito.ArgumentMatchers.any(), eq("seller@example.com")))
                .thenReturn(new com.albertkingdom.shoppingwebsite.model.Product(1L, "Camera", new java.math.BigDecimal("10.00")));

        mockMvc.perform(post("/api/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Camera")
                        .param("productPrice", "10.00"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void productCreate_allowsPurePlatformAdmin() throws Exception {
        when(productService.createProductForActor(org.mockito.ArgumentMatchers.any(), eq("admin@example.com")))
                .thenReturn(new com.albertkingdom.shoppingwebsite.model.Product(1L, "Camera", new java.math.BigDecimal("10.00")));

        mockMvc.perform(post("/api/products")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Camera")
                        .param("productPrice", "10.00"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "seller-b@example.com", authorities = "ROLE_SELLER")
    void productUpdate_rejectsAnotherSeller() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("not owner"))
                .when(productService).verifyProductManagementAccess(7L, "seller-b@example.com");

        mockMvc.perform(put("/api/products/7")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Camera")
                        .param("productPrice", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "seller-b@example.com", authorities = "ROLE_SELLER")
    void productDelete_rejectsAnotherSeller() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("not owner"))
                .when(productService).verifyProductManagementAccess(7L, "seller-b@example.com");

        mockMvc.perform(delete("/api/products/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void productUpdate_allowsPlatformAdmin() throws Exception {
        when(productService.updateProductForActor(org.mockito.ArgumentMatchers.any(), eq(7L), eq("admin@example.com")))
                .thenReturn(new com.albertkingdom.shoppingwebsite.model.Product(7L, "Camera", new java.math.BigDecimal("10.00")));

        mockMvc.perform(put("/api/products/7")
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("productName", "Camera")
                        .param("productPrice", "10.00"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void productDelete_allowsPlatformAdmin() throws Exception {
        com.albertkingdom.shoppingwebsite.model.Product product =
                new com.albertkingdom.shoppingwebsite.model.Product(7L, "Legacy", new java.math.BigDecimal("10.00"));
        when(productService.getProductById(7L)).thenReturn(product);

        mockMvc.perform(delete("/api/products/7"))
                .andExpect(status().isOk());

        verify(productService).deleteProductForActor(7L, "admin@example.com");
    }
}
