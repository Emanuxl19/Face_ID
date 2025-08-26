package com.faceid.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Lob
    @Column(name = "face_features")
    private byte[] faceFeatures;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, byte[] faceFeatures, String profileImagePath) {
        this.username = username;
        this.password = password;
        this.faceFeatures = faceFeatures;
        this.profileImagePath = profileImagePath;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] getFaceFeatures() {
        return faceFeatures;
    }

    public void setFaceFeatures(byte[] faceFeatures) {
        this.faceFeatures = faceFeatures;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }
}
