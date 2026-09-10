package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.ProductResponse;
import com.albertkingdom.shoppingwebsite.exception.ResourceNotFoundException;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product createProductForActor(Product product, String actorEmail) {
        User actor = getUser(actorEmail);
        requireProductCreator(actor);
        if (hasRole(actor, "ROLE_SELLER")) {
            product.setSeller(actor);
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyProductCreationAccess(String actorEmail) {
        requireProductCreator(getUser(actorEmail));
    }

    @Override
    @Transactional
    public Product createPlatformProduct(Product product, String actorEmail) {
        User admin = getUser(actorEmail);
        requireAdmin(admin);
        product.setSeller(null);
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyPlatformProductCreationAccess(String actorEmail) {
        requireAdmin(getUser(actorEmail));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public PageResponse<ProductResponse> getProductsByPage(int page) {
        Pageable pageWithTenElementsDesc = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<Product> result = productRepository.findAll(pageWithTenElementsDesc);
        return PageResponse.of(result, ProductResponse::from);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("product", id));
    }

    @Override
    @Transactional
    public Product updateProduct(Product product, Long id) {
        Product existedProduct = getProductById(id);
        existedProduct.setPrice(product.getPrice());
        existedProduct.setName(product.getName());
        // Nulls signal "no change" — preserve the existing image when the caller
        // did not attach a new one, otherwise the DB row would be blanked out.
        if (product.getImgUrl() != null) {
            existedProduct.setImgUrl(product.getImgUrl());
        }
        if (product.getImgName() != null) {
            existedProduct.setImgName(product.getImgName());
        }
        return saveProduct(existedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("product", id));
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyProductManagementAccess(Long id, String actorEmail) {
        requireProductManager(getProductById(id), getUser(actorEmail));
    }

    @Override
    @Transactional
    public Product updateProductForActor(Product product, Long id, String actorEmail) {
        Product existing = getProductById(id);
        requireProductManager(existing, getUser(actorEmail));
        return updateProduct(product, id);
    }

    @Override
    @Transactional
    public void deleteProductForActor(Long id, String actorEmail) {
        Product existing = getProductById(id);
        requireProductManager(existing, getUser(actorEmail));
        productRepository.delete(existing);
    }

    @Override
    public PageResponse<ProductResponse> getProductsForSeller(String sellerEmail, int page) {
        User seller = getUser(sellerEmail);
        requireSeller(seller);
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        return PageResponse.of(productRepository.findBySellerId(seller.getId(), pageable), ProductResponse::from);
    }

    private User getUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new AccessDeniedException("authenticated user no longer exists");
        }
        return user;
    }

    private void requireSeller(User user) {
        if (!hasRole(user, "ROLE_SELLER")) {
            throw new AccessDeniedException("seller role is required");
        }
    }

    private void requireAdmin(User user) {
        if (!hasRole(user, "ROLE_ADMIN")) {
            throw new AccessDeniedException("platform admin role is required");
        }
    }

    private void requireProductCreator(User user) {
        if (!hasRole(user, "ROLE_SELLER") && !hasRole(user, "ROLE_ADMIN")) {
            throw new AccessDeniedException("seller or platform admin role is required");
        }
    }

    private void requireProductManager(Product product, User actor) {
        if (hasRole(actor, "ROLE_ADMIN")) {
            return;
        }
        requireSeller(actor);
        if (product.getSeller() == null || !actor.getId().equals(product.getSeller().getId())) {
            throw new AccessDeniedException("product is owned by another seller");
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }
}
