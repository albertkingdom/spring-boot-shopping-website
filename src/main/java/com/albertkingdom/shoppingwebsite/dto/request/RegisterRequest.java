package com.albertkingdom.shoppingwebsite.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public shape for POST /api/register. Deliberately exposes only the fields a
 * self-service registration is allowed to set — id, roles, and any future
 * privileged fields on the User entity cannot be injected from the request
 * body.
 */
public class RegisterRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "Not a valid email format.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 6, message = "Password length should be at least 6 characters.")
    private String password;

    @NotBlank(message = "Name should not be empty.")
    private String name;

    public RegisterRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
