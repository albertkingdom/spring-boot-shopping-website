package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.request.RegisterRequest;
import com.albertkingdom.shoppingwebsite.dto.response.UserResponse;
import com.albertkingdom.shoppingwebsite.exception.ConflictException;
import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.RoleRepository;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new ConflictException("email already registered");
        }
        Role defaultRole = roleRepository.findByName("ROLE_USER");
        if (defaultRole == null) {
            throw new IllegalStateException(
                    "ROLE_USER seed row is missing; ensure Flyway V4 has been applied.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.getRoles().add(defaultRole);

        return userRepository.save(user);
    }

    @Override
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public User getUser(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void addRoleToUser(String email, String roleName) {
        User user = userRepository.findByEmail(email);
        Role role = roleRepository.findByName(roleName);
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void grantSellerRole(Long userId, String actorEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        Role sellerRole = roleRepository.findByName("ROLE_SELLER");
        if (sellerRole == null) {
            throw new IllegalStateException("ROLE_SELLER seed row is missing; ensure Flyway V5 has been applied.");
        }
        if (hasRole(user, "ROLE_SELLER")) {
            throw new ConflictException("user is already a seller");
        }
        user.getRoles().add(sellerRole);
        log.info("seller role granted actor={} targetUserId={}", actorEmail, userId);
    }

    @Override
    @Transactional
    public void revokeSellerRole(Long userId, String actorEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        if (!hasRole(user, "ROLE_SELLER")) {
            throw new ConflictException("user is not a seller");
        }
        if (productRepository.existsBySellerId(userId)) {
            throw new ConflictException("seller role cannot be revoked while the user owns products; transfer or remove them first");
        }
        user.getRoles().removeIf(role -> "ROLE_SELLER".equals(role.getName()));
        log.info("seller role revoked actor={} targetUserId={}", actorEmail, userId);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    @Override
    public User getUserByEmailAndPassword(String email, String password) {
        return userRepository.findByEmailAndPassword(email, password);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    /*
    *@override userDetailsService's method
    * how spring security get user info to generate a special UserDetail object which contains user's name
    * and password and roles
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if(user == null) {
            throw new UsernameNotFoundException("User not found in the database");
        }
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        });
        // userDetails.User extends UserDetails
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }
}
