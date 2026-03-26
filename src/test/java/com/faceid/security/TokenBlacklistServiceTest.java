package com.faceid.security;

import com.faceid.model.BlacklistedToken;
import com.faceid.repository.BlacklistedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private BlacklistedTokenRepository repository;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService(repository);
    }

    @Test
    void blacklistShouldSaveHashNotRawToken() {
        String token     = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
        Instant expiry   = Instant.now().plusSeconds(3600);

        service.blacklist(token, expiry);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(repository).save(captor.capture());

        BlacklistedToken saved = captor.getValue();
        // Hash deve ter 64 chars (SHA-256 hex) e não conter o token original
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getTokenHash()).doesNotContain("eyJ");
        assertThat(saved.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void isBlacklistedShouldReturnTrueWhenRepositoryConfirms() {
        String token = "some.jwt.token";
        when(repository.existsByTokenHashAndExpiresAtAfter(any(), any())).thenReturn(true);

        assertThat(service.isBlacklisted(token)).isTrue();
    }

    @Test
    void isBlacklistedShouldReturnFalseWhenNotInRepository() {
        when(repository.existsByTokenHashAndExpiresAtAfter(any(), any())).thenReturn(false);

        assertThat(service.isBlacklisted("valid.token.here")).isFalse();
    }

    @Test
    void sameTokenAlwaysProducesSameHash() {
        String token = "deterministic.token.value";
        Instant expiry = Instant.now().plusSeconds(3600);

        service.blacklist(token, expiry);
        service.blacklist(token, expiry);

        // Ambas as chamadas geram o mesmo hash — o repositório pode tratar como upsert
        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getTokenHash())
                .isEqualTo(captor.getAllValues().get(1).getTokenHash());
    }

    @Test
    void cleanupShouldDelegateToRepository() {
        service.cleanup();
        verify(repository).deleteByExpiresAtBefore(any(Instant.class));
    }
}
