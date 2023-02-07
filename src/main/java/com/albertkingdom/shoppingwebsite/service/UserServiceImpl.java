package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.RoleRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public User saveUser(User user) {
        // todo: encode password
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
        System.out.println("addRoleToUser, user "+ user.getRoles());
        System.out.println("addRoleToUser, role "+ role.getName());
        user.getRoles().add(role);
        System.out.println(user.getRoles());

        userRepository.save(user);
    }

    @Override
    public User getUserByEmailAndPassword(String email, String password) {
        return userRepository.findByEmailAndPassword(email, password);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
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
            System.out.println("User not found in the database");
            throw new UsernameNotFoundException("User not found in the database");
        } else {
            System.out.println("User " + email +" found in the database");
        }
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        });
        // userDetails.User extends UserDetails
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }
}
