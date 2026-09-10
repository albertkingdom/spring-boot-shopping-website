package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/users")
public class AdminSellerController {

    private final UserService userService;

    public AdminSellerController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{userId}/roles/seller")
    public ResponseEntity<Void> grantSellerRole(@PathVariable Long userId, Principal principal) {
        userService.grantSellerRole(userId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles/seller")
    public ResponseEntity<Void> revokeSellerRole(@PathVariable Long userId, Principal principal) {
        userService.revokeSellerRole(userId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
