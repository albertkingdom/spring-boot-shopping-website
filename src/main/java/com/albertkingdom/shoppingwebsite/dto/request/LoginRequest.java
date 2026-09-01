package com.albertkingdom.shoppingwebsite.dto.request;

import javax.validation.constraints.NotBlank;

/**
 * Public shape for POST /api/login. Contains only the credentials required by
 * AuthenticationManager — no way for the client body to influence anything
 * else on the account.
 */
public class LoginRequest {

    @NotBlank(message = "Email is required.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;

    public LoginRequest() {
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
}
