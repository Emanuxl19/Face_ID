package com.faceid.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protege o endpoint de login contra ataques de força bruta via janela deslizante por IP.
 *
 * <p>Regra: se um IP falhar {@value #MAX_ATTEMPTS} vezes dentro de {@code WINDOW},
 * fica bloqueado pelo tempo restante da janela.
 *
 * <p>Implementação: {@link ConcurrentHashMap} em memória — sem dependência externa.
 * Para ambientes multi-instância, substitua por Redis com TTL nativo.
 *
 * <p>Limpeza automática a cada 5 minutos via {@link #cleanup()}.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record AttemptRecord(int count, Instant windowStart) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /** Chamado após login bem-sucedido — remove o histórico de falhas do IP. */
    public void loginSucceeded(String ip) {
        attempts.remove(ip);
    }

    /** Chamado após falha de autenticação — incrementa o contador na janela atual. */
    public void loginFailed(String ip) {
        attempts.merge(ip,
                new AttemptRecord(1, Instant.now()),
                (existing, fresh) -> {
                    // Janela expirou → começa nova contagem
                    if (Instant.now().isAfter(existing.windowStart().plus(WINDOW))) {
                        return new AttemptRecord(1, Instant.now());
                    }
                    return new AttemptRecord(existing.count() + 1, existing.windowStart());
                });
    }

    /**
     * Retorna {@code true} se o IP deve ser bloqueado.
     * Expira automaticamente registros fora da janela.
     */
    public boolean isBlocked(String ip) {
        AttemptRecord record = attempts.get(ip);
        if (record == null) return false;

        if (Instant.now().isAfter(record.windowStart().plus(WINDOW))) {
            attempts.remove(ip);
            return false;
        }
        return record.count() >= MAX_ATTEMPTS;
    }

    /** Retorna quantas tentativas restam antes do bloqueio (para uso em testes/logs). */
    public int remainingAttempts(String ip) {
        AttemptRecord record = attempts.get(ip);
        if (record == null) return MAX_ATTEMPTS;
        if (Instant.now().isAfter(record.windowStart().plus(WINDOW))) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - record.count());
    }

    /** Remove entradas cujas janelas já expiraram. Executa a cada 5 minutos. */
    @Scheduled(fixedDelay = 300_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minus(WINDOW);
        attempts.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff));
    }
}
