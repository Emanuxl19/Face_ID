package com.faceid.service;

import com.faceid.model.AuditEvent;
import com.faceid.model.AuditLog;
import com.faceid.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * Grava eventos de auditoria de forma assíncrona para não bloquear o fluxo principal.
 *
 * <p>O método {@link #log(AuditEvent, Long, String)} usa {@code @Async} — roda em thread
 * separada do pool padrão do Spring. Falhas de persistência são silenciadas com log de erro
 * para nunca derrubar a operação principal.
 *
 * <p><b>Regra de ouro:</b> nunca incluir senhas, tokens JWT, hashes de senha
 * ou dados biométricos no campo {@code details}.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    @Value("${audit.retention-days:90}")
    private int retentionDays;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra um evento com IP extraído automaticamente do contexto da requisição atual.
     */
    @Async
    public void log(AuditEvent event, Long userId, String details) {
        String ip = extractCurrentIp();
        persist(event, userId, ip, details);
    }

    /**
     * Registra um evento com IP explícito (útil quando o IP já foi extraído pelo controller).
     */
    @Async
    public void log(AuditEvent event, Long userId, String ip, String details) {
        persist(event, userId, ip, details);
    }

    private void persist(AuditEvent event, Long userId, String ip, String details) {
        try {
            repository.save(new AuditLog(event, userId, ip, details));
        } catch (Exception e) {
            // Audit log nunca deve derrubar a operação principal
            System.err.println("[AUDIT-FAIL] " + event + " userId=" + userId + " ip=" + ip + " err=" + e.getMessage());
        }
    }

    /**
     * Remove registros de auditoria mais antigos que {@code audit.retention-days} (padrão: 90 dias).
     * Executa diariamente às 3h da manhã.
     * Garante conformidade com políticas de retenção de dados (LGPD/GDPR).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldLogs() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        repository.deleteByTimestampBefore(cutoff);
    }

    private String extractCurrentIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (IllegalStateException e) {
            return "unknown";
        }
    }
}
