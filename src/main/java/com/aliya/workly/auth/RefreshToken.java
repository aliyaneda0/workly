package com.aliya.workly.auth;

import jakarta.persistence.*;

import java.time.Instant;

// One row per refresh token we've ever issued. We never store the token itself, only a
// SHA-256 hash of it (same reasoning as User.passwordHash) — a database leak then can't be
// replayed as a login. See the Workly Auth Blueprint, Section 14, "JWT & refresh tokens".
//
// "family" ties together every token descended from a single login: login issues token A,
// refreshing A issues B, refreshing B issues C ... all share one familyId. If an already-used
// token is presented again (replay), we revoke the whole family at once.
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_refresh_token_family", columnList = "familyId")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String familyId;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    // set the moment this token is rotated away — a used token presented again means replay
    private Instant usedAt;

    // set when the whole family is killed (replay detected, or explicit logout)
    private Instant revokedAt;

    public RefreshToken() {
    }

    // usable == not yet rotated, not revoked, not past its expiry
    public boolean isUsable(Instant now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
