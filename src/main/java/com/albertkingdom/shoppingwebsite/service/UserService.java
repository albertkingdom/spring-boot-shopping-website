package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.request.RegisterRequest;
import com.albertkingdom.shoppingwebsite.dto.response.UserResponse;
import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;

import java.util.List;


public interface UserService {

    /**
     * Register a new self-service account and grant it the default
     * ROLE_USER. Runs in a single transaction so a failure at role
     * assignment cannot leave a role-less account behind.
     *
     * @throws com.albertkingdom.shoppingwebsite.exception.ConflictException
     *         if the email is already registered
     * @throws IllegalStateException
     *         if the ROLE_USER seed row is missing (server misconfiguration)
     */
    User register(RegisterRequest request);

    User saveUser(User user);
    Role saveRole(Role role);
    User getUser(String email);
    void addRoleToUser(String email, String roleName);
    User getUserByEmailAndPassword(String email, String password);
    List<UserResponse> getAllUsers();
}
