package com.faceid.model;

import com.faceid.liveness.ChallengeType;
import com.faceid.liveness.LivenessSessionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Sessão de desafio de anti-spoofing.
 * Os frames da sessão são mantidos em memória no LivenessService (ephemeral, TTL de 45s).
 * Apenas metadados e o resultado final são persistidos aqui.
 */
@Entity
@Table(name = "liveness_sessions")
public class LivenessSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false)
    private ChallengeType challengeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LivenessSessionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Controle de versão para Optimistic Locking do Hibernate.
     * Impede que dois threads concorrentes sobrescrevam o status da sessão silenciosamente.
     * Se addFrame() for chamado simultaneamente, o segundo lançará OptimisticLockException.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /** Resultado serializado como JSON após avaliação concluída. */
    @Column(name = "result_json", columnDefinition = "NVARCHAR(MAX)")
    private String resultJson;

    public LivenessSession() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public ChallengeType getChallengeType() { return challengeType; }
    public void setChallengeType(ChallengeType challengeType) { this.challengeType = challengeType; }

    public LivenessSessionStatus getStatus() { return status; }
    public void setStatus(LivenessSessionStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getVersion() { return version; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
}
