package com.faceid.dto;

public class FaceRecognitionDTO {
    private Long userId;
    private byte[] faceImage; // imagem em bytes

    // Getters e Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public byte[] getFaceImage() { return faceImage; }
    public void setFaceImage(byte[] faceImage) { this.faceImage = faceImage; }
}
