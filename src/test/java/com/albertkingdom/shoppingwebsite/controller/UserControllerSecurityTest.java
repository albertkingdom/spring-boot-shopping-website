package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.SecurityConfig;
import com.albertkingdom.shoppingwebsite.dto.response.UserResponse;
import com.albertkingdom.shoppingwebsite.filter.CustomAuthorizationFilter;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
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

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CustomAuthorizationFilter.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserServiceImpl userServiceImpl;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void listAllUsers_returns403_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void listAllUsers_returns403_whenAuthenticatedButNotAdmin() throws Exception {
        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listAllUsers_returns200_whenAdmin() throws Exception {
        when(userServiceImpl.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listAllUsers_responseHidesPasswordField() throws Exception {
        UserResponse user = new UserResponse(1L, "alice@example.com", "Alice", Arrays.asList("ROLE_USER"));
        when(userServiceImpl.getAllUsers()).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
    }
}
