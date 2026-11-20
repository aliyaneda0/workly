package com.aliya.companyservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Verify-only copy of the monolith's JwtService. company-service never issues tokens — only
// checks ones the monolith's auth code already issued, using the same shared secret (see
// DECISIONS.md ADR-001 in the main app). Once a dedicated auth-service exists as the sole
// issuer, this class stays exactly as-is; only the issuing side changes.
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_COMPANY_ID = "companyId";
    private static final String TYPE_ACCESS = "access";

    @Value("${app.jwt.secret}")
    private String secret;

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        requireType(claims, TYPE_ACCESS);
        return claims;
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey()).build()
                .parseSignedClaims(token).getPayload();
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

    public Long extractCompanyId(Claims claims) {
        return claims.get(CLAIM_COMPANY_ID, Long.class);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
