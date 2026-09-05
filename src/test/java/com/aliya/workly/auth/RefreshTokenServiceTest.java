package com.aliya.workly.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long TTL_SECONDS = 604800; // one week, same as application.properties

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, TTL_SECONDS);
    }

    @Test
    void issueForNewLogin_persistsAToken_andReturnsARawValueThatIsNotWhatWeStored() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.issueForNewLogin(42L);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());

        assertThat(raw).isNotBlank();
        // what we hand the client must never equal what lands in the DB
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(raw);
        assertThat(saved.getValue().getUserId()).isEqualTo(42L);
        assertThat(saved.getValue().getFamilyId()).isNotBlank();
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void rotate_withUnknownToken_throws() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("whatever"))
                .isInstanceOf(BadCredentialsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_withUsableToken_marksItUsed_andMintsAReplacementInTheSameFamily() {
        RefreshToken current = usableToken("fam-1", 7L);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(current));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotation result = service.rotate("presented-raw-token");

        assertThat(current.getUsedAt()).isNotNull();          // old one is spent
        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.newRefreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> minted = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(minted.capture());
        assertThat(minted.getValue().getFamilyId()).isEqualTo("fam-1"); // stays in the family
    }

    @Test
    void rotate_withAlreadyUsedToken_revokesTheWholeFamily_andThrows() {
        RefreshToken replayed = usableToken("fam-2", 9L);
        replayed.setUsedAt(Instant.now().minusSeconds(60)); // this token was already rotated away
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(replayed));

        assertThatThrownBy(() -> service.rotate("stolen-old-token"))
                .isInstanceOf(BadCredentialsException.class);

        verify(repository).revokeFamily(eq("fam-2"), any(Instant.class));
        verify(repository, never()).save(any()); // no new token handed out
    }

    @Test
    void rotate_withExpiredToken_throws_withoutRevokingTheFamily() {
        RefreshToken expired = usableToken("fam-3", 3L);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("old-token"))
                .isInstanceOf(BadCredentialsException.class);

        verify(repository, never()).revokeFamily(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void revokeFamilyOf_unknownToken_isANoOp_notAnError() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatCode(() -> service.revokeFamilyOf("never-seen-this")).doesNotThrowAnyException();
        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void revokeFamilyOf_knownToken_revokesItsFamily() {
        RefreshToken token = usableToken("fam-4", 1L);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.revokeFamilyOf("a-valid-token");

        verify(repository).revokeFamily(eq("fam-4"), any(Instant.class));
    }

    private static RefreshToken usableToken(String familyId, long userId) {
        RefreshToken token = new RefreshToken();
        token.setId(100L);
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setTokenHash("stored-hash");
        token.setIssuedAt(Instant.now().minusSeconds(120));
        token.setExpiresAt(Instant.now().plusSeconds(TTL_SECONDS));
        return token;
    }
}
