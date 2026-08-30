package com.albertkingdom.shoppingwebsite.dto.response;

import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserResponse {
    private final Long id;
    private final String email;
    private final String name;
    private final List<String> roles;

    public UserResponse(Long id, String email, String name, List<String> roles) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.roles = roles;
    }

    public static UserResponse from(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), roleNames);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }
}
