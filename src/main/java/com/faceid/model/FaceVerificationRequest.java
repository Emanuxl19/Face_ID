package com.faceid.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FaceVerificationRequest {

    /**
     * Limite: 1 MB de imagem ≈ ~1.37 MB em Base64 + prefixo data URI (~50 chars).
     * O teto de 1.500.000 chars impede que payloads gigantes sejam desserializados
     * pelo Jackson antes de qualquer validação de negócio.
     */
    @NotBlank(message = "A imagem em Base64 e obrigatoria")
    @Size(max = 1_500_000, message = "Imagem Base64 excede o tamanho permitido (max ~1 MB)")
    private String base64Image;

    // Getters e Setters
    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }
}
