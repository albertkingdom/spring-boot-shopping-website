package com.albertkingdom.shoppingwebsite.repository;

import com.albertkingdom.shoppingwebsite.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmailAndPassword(String email, String password);

    User findByEmail(String email);

    @Query("select u.id as id, u.email as email from User u where u.id in :ids")
    List<BuyerEmailProjection> findBuyerEmailsByIdIn(@Param("ids") Collection<Long> ids);
}
