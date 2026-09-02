package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.exception.ResourceNotFoundException;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.service.CloudinaryService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {
    @MockBean
    ProductService productService;
    @MockBean
    private ProductRepository productRepository;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    UserDetailsService userDetailsService;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    CloudinaryService cloudinaryService;

    @Autowired
    ObjectMapper objectMapper;


    @Test
    void getProductsByPage() throws Exception {
        List<ProductResponse> items = Arrays.asList(
                new ProductResponse(1L, "product1", new BigDecimal("999.00"), null),
                new ProductResponse(2L, "product2", new BigDecimal("999.00"), null),
                new ProductResponse(3L, "product3", new BigDecimal("999.00"), null));
        PageResponse<ProductResponse> result = new PageResponse<>(items, 1, 3L);
        Mockito.when(productService.getProductsByPage(0)).thenReturn(result);

        MvcResult mvcResult = mockMvc.perform(get("/api/products?page=0"))
                .andExpect(status().isOk())
                .andReturn();

        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        log.info(actualJsonResponse);

        String expectedJsonResponse = objectMapper.writeValueAsString(result);
        assertEquals(expectedJsonResponse, actualJsonResponse);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveProduct_shouldReturn200_whenNameAndPriceIsValid() throws Exception {
        Product savedProduct = new Product(7L, "product", new BigDecimal("888.00"));
        Mockito.when(productService.saveProduct(any(Product.class))).thenReturn(savedProduct);

        ProductResponse expected = ProductResponse.from(savedProduct);
        String expectedJsonResponse = objectMapper.writeValueAsString(expected);

        MvcResult mvcResult = mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("productName", "product")
                                .param("productPrice", "888")
                )
                .andExpect(status().isOk())
                .andReturn();
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        log.info("actualJsonResponse{}", actualJsonResponse);
        assertEquals(expectedJsonResponse, actualJsonResponse);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveProduct_shouldReturn400_whenNameOrPriceIsInValid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("productName", "")
                                .param("productPrice", "888")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        log.info("actualJsonResponse{}", actualJsonResponse);
    }

    @Test
    void getProductById() throws Exception {
        Long id = 1L;
        Product product = new Product(id, "product", new BigDecimal("888.00"));
        Mockito.when(productService.getProductById(id)).thenReturn(product);

        MvcResult mvcResult = mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        String expectedJsonResponse = objectMapper.writeValueAsString(ProductResponse.from(product));
        assertEquals(expectedJsonResponse, mvcResult.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", password = "myadmin", roles = "ADMIN")
    void updateProduct() throws Exception {
        Long id = 1L;
        Product product = new Product(id, "product", new BigDecimal("888.00"));
        Mockito.when(productService.updateProduct(any(Product.class), eq(id))).thenReturn(product);

        MvcResult mvcResult = mockMvc.perform(
                        put("/api/products/{id}", id)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("productName", "product")
                                .param("productPrice", "888")
                )
                .andExpect(status().isOk())
                .andReturn();

        String expectedJsonResponse = objectMapper.writeValueAsString(ProductResponse.from(product));
        assertEquals(expectedJsonResponse, mvcResult.getResponse().getContentAsString());
    }

    @Test
    void getProductById_returns404_whenServiceThrowsResourceNotFound() throws Exception {
        Long id = 999L;
        Mockito.when(productService.getProductById(id))
                .thenThrow(new ResourceNotFoundException("product", id));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("product not found: 999"));
    }

    @Test
    void getProductsByPage_returns400_whenPageNegative() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.message contains 'zero')]").exists());
    }

    @Test
    void getProductsByPage_defaultsToZero_whenPageOmitted() throws Exception {
        Mockito.when(productService.getProductsByPage(0))
                .thenReturn(new PageResponse<>(java.util.Collections.emptyList(), 0, 0L));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct() throws Exception {
        Long id = 1L;
        Product testProduct = new Product("test", new BigDecimal("888.00"), "url", "imgName");
        Mockito.doNothing().when(productService).deleteProduct(id);
        Mockito.when(productService.getProductById(id)).thenReturn(testProduct);

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isOk());
        Mockito.verify(productService, Mockito.times(1)).deleteProduct(id);
    }
}
