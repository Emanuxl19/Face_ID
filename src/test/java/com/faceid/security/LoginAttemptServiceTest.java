package com.faceid.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void freshIpShouldNotBeBlocked() {
        assertThat(service.isBlocked("192.168.1.1")).isFalse();
    }

    @Test
    void belowThresholdShouldNotBlock() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.loginFailed("10.0.0.1");
        }
        assertThat(service.isBlocked("10.0.0.1")).isFalse();
        assertThat(service.remainingAttempts("10.0.0.1")).isEqualTo(1);
    }

    @Test
    void exactlyMaxAttemptsShouldBlock() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.loginFailed("10.0.0.2");
        }
        assertThat(service.isBlocked("10.0.0.2")).isTrue();
        assertThat(service.remainingAttempts("10.0.0.2")).isZero();
    }

    @Test
    void successfulLoginShouldClearFailures() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.loginFailed("10.0.0.3");
        }
        assertThat(service.isBlocked("10.0.0.3")).isTrue();

        service.loginSucceeded("10.0.0.3");

        assertThat(service.isBlocked("10.0.0.3")).isFalse();
        assertThat(service.remainingAttempts("10.0.0.3")).isEqualTo(LoginAttemptService.MAX_ATTEMPTS);
    }

    @Test
    void differentIpsShouldBeTrackedIndependently() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.loginFailed("10.0.0.4");
        }
        // IP diferente não deve ser bloqueado
        assertThat(service.isBlocked("10.0.0.5")).isFalse();
    }
}
