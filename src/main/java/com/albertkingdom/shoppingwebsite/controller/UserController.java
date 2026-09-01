package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.request.LoginRequest;
import com.albertkingdom.shoppingwebsite.dto.request.RegisterRequest;
import com.albertkingdom.shoppingwebsite.model.AuthenticationResponse;
import com.albertkingdom.shoppingwebsite.model.CustomResponse;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.service.UserService;
import com.albertkingdom.shoppingwebsite.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


@RestController
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @RequestMapping(value = "/api/register", method = RequestMethod.POST)
    public ResponseEntity<CustomResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return new ResponseEntity<>(new CustomResponse("register success", null), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/login", method = RequestMethod.POST)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) throws Exception {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.debug("login failed for email={}", request.getEmail());
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
        return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
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

                User user = userService.getUser(username);
                String accessToken = jwtUtil.regenerateAccessToken(user);
                return ResponseEntity.ok(new AuthenticationResponse(accessToken, refreshToken, username));


            } catch (Exception exception) {
                log.warn("refresh token validation failed", exception);
                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                return ResponseEntity.status(403).body(error);
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

    }
}
