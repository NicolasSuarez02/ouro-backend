package com.ouro.security;

import com.ouro.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    private static final long REMEMBER_ME_EXPIRATION = 30L * 24 * 60 * 60 * 1000;

    public String generateToken(User user) {
        return generateToken(user, false);
    }

    public String generateToken(User user, boolean rememberMe) {
        long expiration = rememberMe ? REMEMBER_ME_EXPIRATION : jwtExpiration;
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Integer getUserIdFromToken(String token) {
        return Integer.parseInt(validateToken(token).getSubject());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
