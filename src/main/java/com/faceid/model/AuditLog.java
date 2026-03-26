package com.faceid.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registro de auditoria imutável.
 *
 * <p>Cada operação sensível (login, cadastro de face, verificação, acesso negado)
 * gera uma entrada nesta tabela. Os registros nunca são atualizados ou deletados
 * automaticamente — apenas inseridos.
 *
 * <p>Coluna {@code ip_address} aceita até 45 chars para suportar IPv6.
 * Coluna {@code details} armazena contexto livre (ex: similarity score, motivo da negação).
 * <b>Nunca armazenar senhas, tokens ou dados biométricos em {@code details}.</b>
 */
@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user",   columnList = "user_id"),
                @Index(name = "idx_audit_ip",     columnList = "ip_address"),
                @Index(name = "idx_audit_event",  columnList = "event"),
                @Index(name = "idx_audit_ts",     columnList = "timestamp")
        })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AuditEvent event;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(length = 255, updatable = false)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    protected AuditLog() {}

    public AuditLog(AuditEvent event, Long userId, String ipAddress, String details) {
        this.event     = event;
        this.userId    = userId;
        this.ipAddress = ipAddress != null ? ipAddress.substring(0, Math.min(45, ipAddress.length())) : null;
        this.details   = details  != null ? details.substring(0, Math.min(255, details.length())) : null;
        this.timestamp = Instant.now();
    }

    public Long getId()           { return id; }
    public AuditEvent getEvent()  { return event; }
    public Long getUserId()       { return userId; }
    public String getIpAddress()  { return ipAddress; }
    public String getDetails()    { return details; }
    public Instant getTimestamp() { return timestamp; }
}
