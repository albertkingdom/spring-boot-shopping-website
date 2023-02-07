package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.Exception.InvalidRequestException;
import com.albertkingdom.shoppingwebsite.model.AuthenticationResponse;
import com.albertkingdom.shoppingwebsite.model.CustomResponse;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.service.UserServiceImpl;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;


@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;
    @RequestMapping(value = "/api/register", method = RequestMethod.POST)
    public ResponseEntity<CustomResponse> register(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()){
            throw new InvalidRequestException("Invalid parameter", bindingResult);
        }

        /* create a user and add default role as "ROLE_USER"
         */
        userServiceImpl.saveUser(user);

        userServiceImpl.addRoleToUser(user.getEmail(), "ROLE_USER");

        CustomResponse resultResponse = new CustomResponse("register success", null);
        return new ResponseEntity<>(resultResponse, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/login", method = RequestMethod.POST)
    public ResponseEntity<?> login(@RequestBody User user) throws Exception {

        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );
            System.out.println("authentication:"+ authentication);
        } catch (AuthenticationException e) {
            System.out.println("Incorrect username or password: "+ e);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect username or password");
        }
        org.springframework.security.core.userdetails.User authenticatedUser = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        String access_token = jwtUtil.generateAccessToken(authenticatedUser);
        String refresh_token = jwtUtil.generateRefreshToken(authenticatedUser);


        return ResponseEntity.ok(new AuthenticationResponse(access_token, refresh_token, authenticatedUser.getUsername()));
    }

    // todo: if use jwt solution, no need to logout in backend
    @RequestMapping("/api/logout")
    @GetMapping
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "you have log out";
    }

    @RequestMapping("/api/user/all")
    @GetMapping
    public ResponseEntity<?> getAllUser() {
        return new ResponseEntity<>(userServiceImpl.getAllUsers(), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/refreshToken", method = RequestMethod.POST)
    public ResponseEntity<?> getRefreshToken(HttpServletRequest request) {
        // 1. check request header has a refresh token, refresh token only contains subject and expiration time, no roles info
        // 2. extract the username from token to sign an access-token
        String authorizationHeader = request.getHeader(AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refreshToken = authorizationHeader.substring("Bearer ".length());

                DecodedJWT decodedJWT = jwtUtil.decodeJWT(refreshToken);
                String username = decodedJWT.getSubject(); // user email

                User user = userServiceImpl.getUser(username);
                String accessToken = jwtUtil.regenerateAccessToken(user);
                return ResponseEntity.ok(new AuthenticationResponse(accessToken, refreshToken, username));


            } catch (Exception exception) {
                System.out.println("Error logging in: " + exception.getMessage());

                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());

                return ResponseEntity.status(403).body(error);
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

    }
}
