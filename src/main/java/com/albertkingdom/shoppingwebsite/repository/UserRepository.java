package com.albertkingdom.shoppingwebsite.repository;

import com.albertkingdom.shoppingwebsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmailAndPassword(String email, String password);

    User findByEmail(String email);
}
