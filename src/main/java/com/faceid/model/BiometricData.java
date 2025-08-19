package com.faceid.model;

import jakarta.persistence.*;

@Entity
@Table(name = "biometric_data")
public class BiometricData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private byte[] faceTemplate; // Armazena template ou vetor do rosto

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public byte[] getFaceTemplate() { return faceTemplate; }
    public void setFaceTemplate(byte[] faceTemplate) { this.faceTemplate = faceTemplate; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
