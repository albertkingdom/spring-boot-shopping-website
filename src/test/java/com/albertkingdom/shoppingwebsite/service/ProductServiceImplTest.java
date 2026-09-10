package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void createProductForActor_assignsAuthenticatedSellerAsOwner() {
        User seller = userWithRole(7L, "seller@example.com", "ROLE_SELLER");
        Product product = new Product("Camera", new BigDecimal("1200.00"));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(seller);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createProductForActor(product, "seller@example.com");

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertEquals(7L, saved.getValue().getSeller().getId());
    }

    @Test
    void createProductForActor_keepsPlatformProductUnownedForPureAdmin() {
        User admin = userWithRole(1L, "admin@example.com", "ROLE_ADMIN");
        Product product = new Product("Platform item", new BigDecimal("1200.00"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(admin);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = service.createProductForActor(product, "admin@example.com");

        assertEquals(null, saved.getSeller());
    }

    @Test
    void updateProductForActor_rejectsAnotherSeller() {
        Product product = new Product(3L, "Camera", new BigDecimal("1200.00"));
        product.setSeller(userWithRole(7L, "seller-a@example.com", "ROLE_SELLER"));
        User otherSeller = userWithRole(8L, "seller-b@example.com", "ROLE_SELLER");
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller-b@example.com")).thenReturn(otherSeller);

        assertThrows(AccessDeniedException.class, () -> service.updateProductForActor(
                new Product("Changed", new BigDecimal("1000.00")), 3L, "seller-b@example.com"));
    }

    @Test
    void deleteProductForActor_rejectsAnotherSeller() {
        Product product = new Product(3L, "Camera", new BigDecimal("1200.00"));
        product.setSeller(userWithRole(7L, "seller-a@example.com", "ROLE_SELLER"));
        User otherSeller = userWithRole(8L, "seller-b@example.com", "ROLE_SELLER");
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller-b@example.com")).thenReturn(otherSeller);

        assertThrows(AccessDeniedException.class, () -> service.deleteProductForActor(3L, "seller-b@example.com"));

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void verifyProductManagementAccess_allowsPlatformAdminForLegacyProduct() {
        Product legacyProduct = new Product(3L, "Legacy", new BigDecimal("1200.00"));
        User admin = userWithRole(1L, "admin@example.com", "ROLE_ADMIN");
        when(productRepository.findById(3L)).thenReturn(Optional.of(legacyProduct));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(admin);

        service.verifyProductManagementAccess(3L, "admin@example.com");
    }

    @Test
    void updateProductForActor_allowsPlatformAdminForSellerOwnedProduct() {
        Product existing = new Product(3L, "Camera", new BigDecimal("1200.00"));
        existing.setSeller(userWithRole(7L, "seller@example.com", "ROLE_SELLER"));
        User admin = userWithRole(1L, "admin@example.com", "ROLE_ADMIN");
        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(admin);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = service.updateProductForActor(
                new Product("Updated Camera", new BigDecimal("1100.00")), 3L, "admin@example.com");

        assertEquals("Updated Camera", updated.getName());
        assertEquals(7L, updated.getSeller().getId());
    }

    @Test
    void deleteProductForActor_allowsPlatformAdminForLegacyProduct() {
        Product legacyProduct = new Product(3L, "Legacy", new BigDecimal("1200.00"));
        User admin = userWithRole(1L, "admin@example.com", "ROLE_ADMIN");
        when(productRepository.findById(3L)).thenReturn(Optional.of(legacyProduct));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(admin);

        service.deleteProductForActor(3L, "admin@example.com");

        verify(productRepository).delete(legacyProduct);
    }

    private User userWithRole(Long id, String email, String roleName) {
        Role role = new Role(roleName, Collections.emptyList());
        return new User(id, email, "hash", "Name", Collections.singletonList(role));
    }
}
