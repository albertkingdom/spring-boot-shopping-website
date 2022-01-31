package com.albertkingdom.shoppingwebsite.sevice;

import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;
import org.springframework.stereotype.Service;

import java.util.List;


public interface UserService {
    User saveUser(User user);
    Role saveRole(Role role);
    User getUser(String email);
    void addRoleToUser(String email, String roleName);
    User getUserByEmailAndPassword(String email, String password);
    List<User> getAllUsers();
}
