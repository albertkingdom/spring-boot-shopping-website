package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.exception.ConflictException;
import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.repository.RoleRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private UserServiceImpl service;

    @Test
    void grantSellerRole_addsSellerRoleToTargetUser() {
        User user = new User(2L, "seller@example.com", "hash", "Seller", new ArrayList<>());
        Role sellerRole = new Role("ROLE_SELLER", Collections.emptyList());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_SELLER")).thenReturn(sellerRole);

        service.grantSellerRole(2L, "admin@example.com");

        assertEquals("ROLE_SELLER", user.getRoles().iterator().next().getName());
    }

    @Test
    void revokeSellerRole_rejectsSellerWhoStillOwnsProducts() {
        Role sellerRole = new Role("ROLE_SELLER", Collections.emptyList());
        User seller = new User(2L, "seller@example.com", "hash", "Seller", Collections.singletonList(sellerRole));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(productRepository.existsBySellerId(2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.revokeSellerRole(2L, "admin@example.com"));
    }
}
