package com.albertkingdom.shoppingwebsite.util;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";
    private static final String ISSUER = "test-issuer";
    private static final String AUDIENCE = "test-audience";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, ISSUER, AUDIENCE);
    private final JwtUtil otherIssuer = new JwtUtil(SECRET, "other-issuer", AUDIENCE);
    private final JwtUtil otherAudience = new JwtUtil(SECRET, ISSUER, "other-audience");

    private static User principal() {
        return new User("alice@example.com", "unused",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void accessTokenVerifiesAsAccess() {
        String access = jwtUtil.generateAccessToken(principal());
        DecodedJWT decoded = jwtUtil.verify(access, JwtUtil.TokenType.ACCESS);
        assertEquals("alice@example.com", decoded.getSubject());
        assertEquals(JwtUtil.TYPE_ACCESS, decoded.getClaim(JwtUtil.CLAIM_TYPE).asString());
        assertEquals(ISSUER, decoded.getIssuer());
    }

    @Test
    void refreshTokenVerifiesAsRefresh() {
        String refresh = jwtUtil.generateRefreshToken(principal());
        DecodedJWT decoded = jwtUtil.verify(refresh, JwtUtil.TokenType.REFRESH);
        assertEquals(JwtUtil.TYPE_REFRESH, decoded.getClaim(JwtUtil.CLAIM_TYPE).asString());
    }

    @Test
    void accessTokenRejectedWhenVerifiedAsRefresh() {
        String access = jwtUtil.generateAccessToken(principal());
        assertThrows(InvalidClaimException.class,
                () -> jwtUtil.verify(access, JwtUtil.TokenType.REFRESH));
    }

    @Test
    void refreshTokenRejectedWhenVerifiedAsAccess() {
        String refresh = jwtUtil.generateRefreshToken(principal());
        assertThrows(InvalidClaimException.class,
                () -> jwtUtil.verify(refresh, JwtUtil.TokenType.ACCESS));
    }

    @Test
    void tokenFromWrongIssuerRejected() {
        String foreign = otherIssuer.generateAccessToken(principal());
        assertThrows(RuntimeException.class,
                () -> jwtUtil.verify(foreign, JwtUtil.TokenType.ACCESS));
    }

    @Test
    void tokenFromWrongAudienceRejected() {
        String foreign = otherAudience.generateAccessToken(principal());
        assertThrows(RuntimeException.class,
                () -> jwtUtil.verify(foreign, JwtUtil.TokenType.ACCESS));
    }
}
