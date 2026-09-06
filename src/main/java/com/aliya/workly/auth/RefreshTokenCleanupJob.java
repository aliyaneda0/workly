package com.aliya.workly.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Periodically deletes long-expired refresh_token rows so the table doesn't grow without
// bound — every login and every refresh ever performed leaves a row otherwise. Nothing here
// is security-critical: an expired row is inert, and rotate() already ignores it.
//
// A datastore with per-key TTL (Redis) would make this job unnecessary — deferred on purpose,
// see DECISIONS.md ADR-007.
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenService refreshTokenService;
    private final Duration retentionAfterExpiry;

    public RefreshTokenCleanupJob(
            RefreshTokenService refreshTokenService,
            @Value("${app.refresh-token.retention-after-expiry:P7D}") Duration retentionAfterExpiry) {
        this.refreshTokenService = refreshTokenService;
        this.retentionAfterExpiry = retentionAfterExpiry;
    }

    // Cron, not fixedRate: this is date-based tidying, not a heartbeat — running it once in the
    // small hours is plenty, and a fixedRate would also fire on every app restart. Default is
    // 03:30 daily; override with app.refresh-token.cleanup-cron.
    @Scheduled(cron = "${app.refresh-token.cleanup-cron:0 30 3 * * *}")
    public void purgeExpired() {
        int deleted = refreshTokenService.deleteExpiredOlderThan(retentionAfterExpiry);
        if (deleted > 0) {
            log.info("Refresh-token cleanup: deleted {} row(s) expired more than {} ago",
                    deleted, retentionAfterExpiry);
        }
    }
}
