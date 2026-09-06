package com.aliya.workly.security;

import com.aliya.workly.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Signs and verifies access tokens (HS256, one shared secret — see DECISIONS.md ADR-001).
// Every token carries a "typ":"access" claim, so even if some other kind of signed token
// leaks into an Authorization header it won't be accepted here — see the security checklist
// entry on token-type confusion in the Workly Auth Blueprint, Section 14. Refresh tokens are
// NOT JWTs — they're opaque handles managed by RefreshTokenService.
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-ttl-seconds}")
    private long accessTtlSeconds;

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, TYPE_ACCESS, accessTtlSeconds);
    }

    private String buildToken(User user, String type, long ttlSeconds) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_UID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlSeconds * 1000))
                .signWith(signingKey())
                .compact();
    }

    // Throws JwtException (bad signature, malformed, or expired) — callers decide what that means.
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        requireType(claims, TYPE_ACCESS);
        return claims;
    }

    private void requireType(Claims claims, String expected) {
        Object actual = claims.get(CLAIM_TYPE);
        if (!expected.equals(actual)) {
            throw new JwtException("Expected a " + expected + " token but got " + actual);
        }
    }

    public Long extractUserId(Claims claims) {
        return claims.get(CLAIM_UID, Long.class);
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
