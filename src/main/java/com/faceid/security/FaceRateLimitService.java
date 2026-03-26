package com.faceid.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter para os endpoints de verificação facial.
 *
 * <p>Previne <b>oracle attacks</b>: sem limite, um atacante poderia enviar
 * milhares de rostos diferentes contra um userId e deduzir a identidade
 * pela resposta de similarity score.
 *
 * <p>Limite: {@value #MAX_ATTEMPTS} verificações por IP por hora.
 * A janela é reiniciada automaticamente após {@link #WINDOW}.
 *
 * <p>Igual ao {@link LoginAttemptService} mas com thresholds mais permissivos
 * (biometria é mais lenta, falsos negativos são comuns em má iluminação).
 */
@Service
public class FaceRateLimitService {

    public static final int      MAX_ATTEMPTS = 30;
    public static final Duration WINDOW       = Duration.ofHours(1);

    private record AttemptRecord(int count, Instant windowStart) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        AttemptRecord r = attempts.get(ip);
        if (r == null) return false;
        if (Instant.now().isAfter(r.windowStart().plus(WINDOW))) {
            attempts.remove(ip);
            return false;
        }
        return r.count() >= MAX_ATTEMPTS;
    }

    public void recordAttempt(String ip) {
        attempts.merge(ip,
                new AttemptRecord(1, Instant.now()),
                (existing, fresh) -> Instant.now().isAfter(existing.windowStart().plus(WINDOW))
                        ? new AttemptRecord(1, Instant.now())
                        : new AttemptRecord(existing.count() + 1, existing.windowStart()));
    }

    public int remainingAttempts(String ip) {
        AttemptRecord r = attempts.get(ip);
        if (r == null) return MAX_ATTEMPTS;
        if (Instant.now().isAfter(r.windowStart().plus(WINDOW))) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - r.count());
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minus(WINDOW);
        attempts.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff));
    }
}
