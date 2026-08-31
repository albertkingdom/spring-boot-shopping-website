package com.albertkingdom.shoppingwebsite;

import com.albertkingdom.shoppingwebsite.controller.ProductController;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.service.CloudinaryService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CORSConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000,https://shop.example.com")
class CORSConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;
    @MockBean
    private ProductRepository productRepository;
    @MockBean
    private CloudinaryService cloudinaryService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void preflight_fromAllowedOrigin_returnsAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header(ORIGIN, "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflight_fromAnotherAllowedOrigin_returnsAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header(ORIGIN, "https://shop.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://shop.example.com"));
    }

    @Test
    void preflight_fromDisallowedOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header(ORIGIN, "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
