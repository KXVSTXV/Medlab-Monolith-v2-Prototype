package com.cognizant.medlab.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT utility — token creation, validation, and claim extraction.
 *
 * Uses jjwt 0.12.x API (Jwts.parser(), not deprecated Jwts.parserBuilder()).
 *
 * Upgrade path v2→v3: swap secret-based HMAC for RSA keypair when
 * Identity Service becomes a standalone micro-service or Keycloak is introduced.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Token generation ──────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                   .subject(userDetails.getUsername())
                   .claim("authorities", userDetails.getAuthorities()
                           .stream()
                           .map(a -> a.getAuthority())
                           .toList())
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                   .signWith(getSigningKey())
                   .compact();
    }

    // ── Claim extraction ──────────────────────────────────────────

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Internals ────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(getSigningKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    private SecretKey getSigningKey() {
        // If secret is not Base64-encoded, hash it to 256 bits
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes();
        }
        // Ensure minimum 256-bit key length for HMAC-SHA256
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
