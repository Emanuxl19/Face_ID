package com.faceid.model;

/**
 * Poses faciais reconhecidas durante o cadastro multi-ângulo.
 *
 * Cada pose representa uma orientação distinta do rosto:
 *   FRONT  — rosto centralizado, olhando diretamente para a câmera
 *   LEFT   — cabeça girada para a esquerda do usuário
 *   RIGHT  — cabeça girada para a direita do usuário
 *   UP     — cabeça inclinada para cima
 *   DOWN   — cabeça inclinada para baixo
 */
public enum FacePose {
    FRONT("Olhe diretamente para a câmera"),
    LEFT("Vire a cabeça levemente para a esquerda"),
    RIGHT("Vire a cabeça levemente para a direita"),
    UP("Incline a cabeça levemente para cima"),
    DOWN("Incline a cabeça levemente para baixo");

    private final String instruction;

    FacePose(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}
