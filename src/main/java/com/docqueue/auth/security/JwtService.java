package com.docqueue.auth.security;

import com.docqueue.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT service for token generation, validation, and blacklisting.
 * Uses Redis for revoked token tracking (logout / forced expiry).
 */
@Slf4j
@Service
public class JwtService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiry-ms}")
    private long accessExpiryMs;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    private final StringRedisTemplate redisTemplate;

    public JwtService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private SecretKey signingKey() {
        byte[] decoded = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(decoded);
    }

    // ===== Token Generation =====

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpiryMs, "ACCESS");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpiryMs, "REFRESH");
    }

    private String buildToken(User user, long expiryMs, String type) {
        String role = user.getRoles().stream()
                .map(r -> "ROLE_" + r.getName())
                .findFirst()
                .orElse("ROLE_PATIENT");

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("name",   user.getName())
                .claim("role",   role)
                .claim("type",   type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(signingKey())
                .compact();
    }

    // ===== Token Validation =====

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            if (isBlacklisted(claims.getId() != null ? claims.getId() : token)) return false;
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // ===== Blacklisting (Logout) =====

    public void blacklistToken(String token) {
        try {
            Claims claims    = parseClaims(token);
            long  ttlMillis  = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttlMillis > 0) {
                redisTemplate.opsForValue()
                        .set(BLACKLIST_PREFIX + token.substring(token.length() - 16),
                             "revoked",
                             ttlMillis,
                             TimeUnit.MILLISECONDS);
            }
        } catch (JwtException ex) {
            log.warn("Could not blacklist token: {}", ex.getMessage());
        }
    }

    private boolean isBlacklisted(String key) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + key));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
