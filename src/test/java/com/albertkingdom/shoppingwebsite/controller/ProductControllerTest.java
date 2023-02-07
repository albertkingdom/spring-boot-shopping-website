package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.model.ProductsPagination;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        List<Product> listProducts = new ArrayList<>();
        listProducts.add(new Product("product1",999F));
        listProducts.add(new Product("product2",999F));
        listProducts.add(new Product("product3",999F));

        ProductsPagination result = new ProductsPagination(listProducts, 1, 3L);
        Mockito.when(productService.getProductsByPage(0)).thenReturn(result); //模擬controller method會調用到的service方法回傳值

        String url = "/api/products?page=0";

        MvcResult mvcResult = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn(); //模擬get request

        String actualJsonResponse = mvcResult.getResponse().getContentAsString(); //模擬get request請求的response
        log.info(actualJsonResponse);

        String expectedJsonResponse = objectMapper.writeValueAsString(result);

        assertEquals(expectedJsonResponse, actualJsonResponse); //assert兩者結果相同
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveProduct_shouldReturn200_whenNameAndPriceIsValid() throws Exception {
        Product product = new Product();
        product.setName("product");
        product.setPrice(888F);
        Product savedProduct = new Product(null,"product",888F);


        // Mock the result from service
        Mockito.when(productService.saveProduct(any(Product.class))).thenReturn(savedProduct);

        String url = "/api/products/";
        String expectedJsonResponse = objectMapper.writeValueAsString(savedProduct);

        MvcResult mvcResult = mockMvc.perform(
                        post(url)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("productName", "product")
                                .param("productPrice", "888")
                )
                .andExpect(status().isOk())
                .andReturn();
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        System.out.println("actualJsonResponse" + actualJsonResponse);
        assertEquals(expectedJsonResponse, actualJsonResponse); //assert兩者結果相同


    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void saveProduct_shouldReturn400_whenNameOrPriceIsInValid() throws Exception {

        String url = "/api/products/";


        MvcResult mvcResult = mockMvc.perform(
                        post(url)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("productName", "")
                                .param("productPrice", "888")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        System.out.println("actualJsonResponse" + actualJsonResponse);

    }
    @Test
    void getProductById() throws Exception {
        Long id = 1L; //specify an id
        Product product = new Product();
        product.setId(id);
        Mockito.when(productService.getProductById(id)).thenReturn(product);

        // get()的第2個參數是path variable值，可以有多個值
        MvcResult mvcResult = mockMvc.perform(get("/api/products/{id}",id)).andExpect(status().isOk()).andReturn(); // real response

        String expectedJsonResponse = objectMapper.writeValueAsString(product); //transform expected product object to string

        assertEquals(expectedJsonResponse, mvcResult.getResponse().getContentAsString());

    }

    @Test
    @WithMockUser(username = "admin@gmail.com", password = "myadmin", roles = "ADMIN")
    void updateProduct() throws Exception {
        Long id = 1L;
        Product product = new Product(1L,"product",888F);

        Mockito.when(productService.updateProduct(any(Product.class), eq(id))).thenReturn(product);

        try {
            MvcResult mvcResult = mockMvc.perform(
                    put("/api/products/{id}", id)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .param("productName", "product")
                            .param("productPrice", "888")

            ).andReturn();
            assertEquals(200,mvcResult.getResponse().getStatus());
            String expectedJsonResponse = objectMapper.writeValueAsString(product); //transform expected product object to string
            String actualJsonResponse = mvcResult.getResponse().getContentAsString();
            assertEquals(expectedJsonResponse, actualJsonResponse);
        } catch (Exception e) {
            System.out.println(e);
        }


    }

    @Test
    void deleteProduct() throws Exception {
        Long id = 1L;
        Product testProduct = new Product("test",888F,"url", "imgName");
        Mockito.doNothing().when(productService).deleteProduct(id);

        Mockito.when(productService.getProductById(id)).thenReturn(testProduct);

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isOk());
        //productServiceImpl.deleteProduct is called 1 time
        Mockito.verify(productService, Mockito.times(1)).deleteProduct(id);
    }
}