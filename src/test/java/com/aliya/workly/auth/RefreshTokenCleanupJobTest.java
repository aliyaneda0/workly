package com.aliya.workly.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void purgeExpired_passesTheConfiguredRetentionThrough() {
        RefreshTokenCleanupJob job = new RefreshTokenCleanupJob(refreshTokenService, Duration.ofDays(7));
        when(refreshTokenService.deleteExpiredOlderThan(any())).thenReturn(0);

        job.purgeExpired();

        verify(refreshTokenService).deleteExpiredOlderThan(Duration.ofDays(7));
    }
}
