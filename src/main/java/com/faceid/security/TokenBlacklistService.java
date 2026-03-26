package com.faceid.security;

import com.faceid.model.BlacklistedToken;
import com.faceid.repository.BlacklistedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Gerencia a blacklist de tokens JWT invalidados (logout).
 *
 * <p><b>Por que hash e não token bruto?</b> O SHA-256 do token é suficiente para lookup
 * e garante que o token original não possa ser recuperado do banco — defesa em profundidade.
 *
 * <p><b>Custo por request:</b> uma query {@code EXISTS} com PK lookup.
 * Para escala, substituir a tabela por Redis com TTL nativo.
 *
 * <p>A limpeza automática remove entradas cujos JWTs originais já expiraram —
 * tokens expirados são inúteis mesmo sem estar na blacklist.
 */
@Service
public class TokenBlacklistService {

    private final BlacklistedTokenRepository repository;

    public TokenBlacklistService(BlacklistedTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Invalida o token. Chamado no logout.
     *
     * @param token     Token JWT bruto (nunca armazenado).
     * @param expiresAt Expiração original do JWT (para limpeza automática).
     */
    @Transactional
    public void blacklist(String token, Instant expiresAt) {
        repository.save(new BlacklistedToken(hash(token), expiresAt));
    }

    /**
     * Retorna {@code true} se o token foi explicitamente invalidado e ainda não expirou.
     * Chamado pelo {@link JwtAuthFilter} em toda requisição autenticada.
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        return repository.existsByTokenHashAndExpiresAtAfter(hash(token), Instant.now());
    }

    /** Remove entradas cujos JWTs originais já expiraram. Executa a cada hora. */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cleanup() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
