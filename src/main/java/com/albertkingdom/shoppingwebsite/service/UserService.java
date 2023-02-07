package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;

import java.util.List;


public interface UserService {
    User saveUser(User user);
    Role saveRole(Role role);
    User getUser(String email);
    void addRoleToUser(String email, String roleName);
    User getUserByEmailAndPassword(String email, String password);
    List<User> getAllUsers();
}
