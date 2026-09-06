package com.aliya.workly.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

// Owns the lifecycle of refresh tokens: minting them, rotating them on every use, and
// killing a whole family when a used token shows up again (replay). AuthService talks to
// this; nothing else should.
@Service
public class RefreshTokenService {

    // 256 bits of randomness, url-safe. Long enough that guessing is hopeless; the value is
    // never parsed, only hashed and looked up.
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTtlSeconds;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-token-ttl-seconds}") long refreshTtlSeconds) {
        this.repository = repository;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    // Login / register: start a brand-new family and hand back the raw token.
    @Transactional
    public String issueForNewLogin(Long userId) {
        return mint(userId, UUID.randomUUID().toString());
    }

    // /auth/refresh: validate the presented token, rotate it, return {userId, newRawToken}.
    //
    // noRollbackFor is deliberate and important: when we detect replay we call revokeFamily()
    // and THEN throw. Without this, the thrown exception would roll back the transaction and
    // undo the revocation — the attacker's replay would fail but the family would stay alive.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Rotation rotate(String presentedToken) {
        Instant now = Instant.now();
        RefreshToken stored = repository.findByTokenHash(hash(presentedToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (stored.getUsedAt() != null || stored.getRevokedAt() != null) {
            // A token we already rotated away (or revoked) is being presented again. Either an
            // attacker stole it, or the legit client's newer token was stolen and this is the
            // client retrying an old one. We can't tell which, so we assume the worst and burn
            // the whole family — everyone re-authenticates.
            repository.revokeFamily(stored.getFamilyId(), now);
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        if (stored.getExpiresAt().isBefore(now)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        stored.setUsedAt(now); // managed entity — flushed on commit
        String newToken = mint(stored.getUserId(), stored.getFamilyId());
        return new Rotation(stored.getUserId(), newToken);
    }

    // /auth/logout: kill the family this token belongs to. Idempotent — an unknown or already
    // dead token is a no-op, so logging out twice (or with a stale token) never errors.
    @Transactional
    public void revokeFamilyOf(String presentedToken) {
        repository.findByTokenHash(hash(presentedToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    // Housekeeping (RefreshTokenCleanupJob): delete rows whose token expired more than
    // `retention` ago. We don't delete the instant a token expires: rotate() checks
    // used/revoked *before* expiry, so a token that was used and is then replayed still trips
    // family revocation even after it's expired. The retention window is how long that
    // replay-detection signal outlives the token. Returns the number of rows deleted.
    @Transactional
    public int deleteExpiredOlderThan(Duration retention) {
        return repository.deleteExpiredBefore(Instant.now().minus(retention));
    }

    private String mint(Long userId, String familyId) {
        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        Instant now = Instant.now();
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hash(token));
        entity.setUserId(userId);
        entity.setFamilyId(familyId);
        entity.setIssuedAt(now);
        entity.setExpiresAt(now.plusSeconds(refreshTtlSeconds));
        repository.save(entity);

        return token;
    }

    // SHA-256 (not bcrypt): the input is already 256 bits of uniform randomness, so there's
    // nothing to brute-force — a slow hash would just waste CPU on every refresh. Hex so the
    // stored value is a predictable length and safe as a unique-index key.
    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // userId travels back so AuthService doesn't have to re-parse anything to know who this is.
    public record Rotation(Long userId, String newRefreshToken) {
    }
}
