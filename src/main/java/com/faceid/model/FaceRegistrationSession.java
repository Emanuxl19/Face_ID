package com.faceid.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sessão de cadastro facial multi-ângulo.
 *
 * Controla quais poses já foram capturadas e o status geral do cadastro.
 * Usa @Version para locking otimista — evita race conditions quando o frontend
 * envia vários frames simultaneamente.
 *
 * As poses coletadas são armazenadas como CSV (ex: "FRONT,LEFT") por simplicidade;
 * use {@link #getCollectedPosesAsSet()} e {@link #setCollectedPosesFromSet(Set)} para acessá-las.
 */
@Entity
@Table(name = "face_registration_sessions")
public class FaceRegistrationSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FaceRegistrationStatus status;

    /** Poses coletadas, separadas por vírgula. Ex: "FRONT,LEFT,RIGHT" */
    @Column(name = "collected_poses", length = 100)
    private String collectedPoses;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Locking otimista — evita dois frames atualizando a mesma sessão simultaneamente. */
    @Version
    private Long version;

    public FaceRegistrationSession() {}

    // ── Helpers para collectedPoses ─────────────────────────────────────────

    public Set<FacePose> getCollectedPosesAsSet() {
        if (collectedPoses == null || collectedPoses.isBlank()) return new HashSet<>();
        return Arrays.stream(collectedPoses.split(","))
                .filter(s -> !s.isBlank())
                .map(FacePose::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public void setCollectedPosesFromSet(Set<FacePose> poses) {
        this.collectedPoses = poses.stream()
                .map(FacePose::name)
                .collect(Collectors.joining(","));
    }

    // ── Getters / Setters ───────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public FaceRegistrationStatus getStatus() { return status; }
    public void setStatus(FaceRegistrationStatus status) { this.status = status; }

    public String getCollectedPoses() { return collectedPoses; }
    public void setCollectedPoses(String collectedPoses) { this.collectedPoses = collectedPoses; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getVersion() { return version; }
}
