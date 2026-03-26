package com.faceid.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Armazena as características faciais (PNG 100×100 grayscale) de um usuário
 * para um ângulo específico de pose.
 *
 * Um usuário pode ter várias entradas — uma por pose (FRONT, LEFT, RIGHT, ...).
 * A verificação compara o rosto enviado contra TODAS as poses registradas e
 * aceita se qualquer uma delas superar o limiar de similaridade.
 */
@Entity
@Table(name = "face_angle_images",
        indexes = @Index(name = "idx_face_angle_user", columnList = "user_id"))
public class FaceAngleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FacePose pose;

    @Lob
    @Column(name = "face_features", nullable = false)
    @JsonIgnore
    private byte[] faceFeatures;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FaceAngleImage() {}

    public FaceAngleImage(Long userId, FacePose pose, byte[] faceFeatures) {
        this.userId = userId;
        this.pose = pose;
        this.faceFeatures = faceFeatures;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public FacePose getPose() { return pose; }
    public void setPose(FacePose pose) { this.pose = pose; }

    public byte[] getFaceFeatures() { return faceFeatures; }
    public void setFaceFeatures(byte[] faceFeatures) { this.faceFeatures = faceFeatures; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
