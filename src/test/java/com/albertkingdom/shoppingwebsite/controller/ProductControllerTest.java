package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.sevice.CloudinaryService;
import com.albertkingdom.shoppingwebsite.sevice.ProductServiceImpl;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.PostRemove;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {
    @MockBean
    ProductServiceImpl productServiceImpl;
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
    void getAllProducts() throws Exception {
        List<Product> listProducts = new ArrayList<>();
        listProducts.add(new Product("product1",999F));
        listProducts.add(new Product("product2",999F));
        listProducts.add(new Product("product3",999F));

        Mockito.when(productServiceImpl.getAllProducts()).thenReturn(listProducts); //??????controller method???????????????service???????????????

        String url = "/api/products";

        MvcResult mvcResult = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn(); //??????get request

        String actualJsonResponse = mvcResult.getResponse().getContentAsString(); //??????get request?????????response
        System.out.println(actualJsonResponse);

        String expectedJsonResponse = objectMapper.writeValueAsString(listProducts);

        assertEquals(expectedJsonResponse, actualJsonResponse); //assert??????????????????
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void saveProduct_shouldReturn200_whenNameAndPriceIsValid() throws Exception {
        Product product = new Product();
        product.setName("product");
        product.setPrice(888F);
        Product savedProduct = new Product(null,"product",888F);


        // Mock the result from service
        Mockito.when(productServiceImpl.saveProduct(any(Product.class))).thenReturn(savedProduct);

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
        assertEquals(expectedJsonResponse, actualJsonResponse); //assert??????????????????


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
        Mockito.when(productServiceImpl.getProductById(id)).thenReturn(product);

        // get()??????2????????????path variable????????????????????????
        MvcResult mvcResult = mockMvc.perform(get("/api/products/{id}",id)).andExpect(status().isOk()).andReturn(); // real response

        String expectedJsonResponse = objectMapper.writeValueAsString(product); //transform expected product object to string

        assertEquals(expectedJsonResponse, mvcResult.getResponse().getContentAsString());

    }

    @Test
    @WithMockUser(username = "admin@gmail.com", password = "myadmin", roles = "ADMIN")
    void updateProduct() throws Exception {
        Long id = 1L;
        Product product = new Product(1L,"product",888F);

        Mockito.when(productServiceImpl.updateProduct(any(Product.class), eq(id))).thenReturn(product);

        try {
            MvcResult mvcResult = mockMvc.perform(
                    put("/api/products/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(product))
            ).andExpect(status().isOk()).andReturn();
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
        Mockito.doNothing().when(productServiceImpl).deleteProduct(id);

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isOk());
        //productServiceImpl.deleteProduct is called 1 time
        Mockito.verify(productServiceImpl, Mockito.times(1)).deleteProduct(id);
    }
}