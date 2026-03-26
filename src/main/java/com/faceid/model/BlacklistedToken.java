package com.faceid.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Token JWT invalidado explicitamente via logout.
 *
 * <p>O token bruto nunca é armazenado — apenas seu SHA-256 em hex (64 chars).
 * Isso garante que mesmo com acesso direto ao banco, o token original não pode ser recuperado.
 *
 * <p>{@code expiresAt} reflete quando o JWT original expiraria.
 * Registros são limpos automaticamente após a expiração (ver {@link com.faceid.security.TokenBlacklistService}).
 */
@Entity
@Table(name = "token_blacklist")
public class BlacklistedToken {

    /** SHA-256 hex do token JWT (64 chars). Usado como PK para lookup O(1) pelo índice PK. */
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    private Instant blacklistedAt;

    protected BlacklistedToken() {}

    public BlacklistedToken(String tokenHash, Instant expiresAt) {
        this.tokenHash     = tokenHash;
        this.expiresAt     = expiresAt;
        this.blacklistedAt = Instant.now();
    }

    public String getTokenHash()      { return tokenHash; }
    public Instant getExpiresAt()     { return expiresAt; }
    public Instant getBlacklistedAt() { return blacklistedAt; }
}
