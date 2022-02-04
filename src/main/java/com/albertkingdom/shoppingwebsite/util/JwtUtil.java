package com.albertkingdom.shoppingwebsite.util;

import com.albertkingdom.shoppingwebsite.model.Role;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtUtil {

    private final String SECRET_KEY = "secret";
    Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
    public String generateAccessToken(User authenticatedUser) {

        String access_token = JWT.create().withSubject(authenticatedUser.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000L))
                .withClaim("roles", authenticatedUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .sign(algorithm);
        return access_token;
    }

    public String generateRefreshToken(User authenticatedUser) {
        String refresh_token = JWT.create().withSubject(authenticatedUser.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))
                .sign(algorithm);
        return refresh_token;
    }

    public DecodedJWT decodeJWT(String token) {

        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decodedJWT = verifier.verify(token);
        System.out.println("decodedJWT: "+ decodedJWT);
        return decodedJWT;
    }

    public String regenerateAccessToken(com.albertkingdom.shoppingwebsite.model.User user) {
        String access_token = JWT.create().withSubject(user.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .withClaim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .sign(algorithm);
        return access_token;
    }
}
