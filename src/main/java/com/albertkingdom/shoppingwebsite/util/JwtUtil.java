package com.albertkingdom.shoppingwebsite.util;

import com.albertkingdom.shoppingwebsite.model.Role;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 32;

    private static final long ACCESS_TOKEN_TTL_MS = 10 * 60 * 1000L;             // 10 minutes
    private static final long REFRESH_TOKEN_TTL_MS = 24 * 60 * 60 * 1000L;       // 24 hours

    /** Token type claim; enforced on verify to prevent using a refresh token as an access token. */
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public enum TokenType {
        ACCESS(TYPE_ACCESS), REFRESH(TYPE_REFRESH);

        private final String claimValue;

        TokenType(String claimValue) {
            this.claimValue = claimValue;
        }

        public String claimValue() {
            return claimValue;
        }
    }

    private final Algorithm algorithm;
    private final String issuer;
    private final String audience;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.issuer:shopping-website}") String issuer,
                   @Value("${jwt.audience:shopping-website-api}") String audience) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                    "jwt.secret must be configured (set the JWT_SECRET environment variable).");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long.");
        }
        this.algorithm = Algorithm.HMAC256(secretBytes);
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateAccessToken(User authenticatedUser) {
        return JWT.create()
                .withSubject(authenticatedUser.getUsername())
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim(CLAIM_TYPE, TYPE_ACCESS)
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
                .withClaim("roles", authenticatedUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .sign(algorithm);
    }

    public String generateRefreshToken(User authenticatedUser) {
        return JWT.create()
                .withSubject(authenticatedUser.getUsername())
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim(CLAIM_TYPE, TYPE_REFRESH)
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS))
                .sign(algorithm);
    }

    public String regenerateAccessToken(com.albertkingdom.shoppingwebsite.model.User user) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim(CLAIM_TYPE, TYPE_ACCESS)
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
                .withClaim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .sign(algorithm);
    }

    /**
     * Verify a token's signature, expiry, issuer, audience, AND that its type
     * claim matches {@code expected}. Rejects using a refresh token where an
     * access token is required and vice versa.
     */
    public DecodedJWT verify(String token, TokenType expected) {
        return JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim(CLAIM_TYPE, expected.claimValue())
                .build()
                .verify(token);
    }
}
