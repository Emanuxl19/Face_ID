package com.faceid.liveness;

/**
 * Tipos de desafio para detecção de vivacidade (anti-spoofing).
 *
 * PASSIVE    - Análise de frame único sem interação do usuário.
 * BLINK      - Usuário deve piscar os olhos (rastreamento de brilho da região ocular).
 * HEAD_NOD   - Usuário deve balançar a cabeça verticalmente (fluxo óptico + centroide).
 * HEAD_SHAKE - Usuário deve balançar a cabeça horizontalmente (fluxo óptico + centroide).
 */
public enum ChallengeType {

    PASSIVE("Olhe diretamente para a camera por 3 segundos.", 1),
    BLINK("Pisque os olhos duas vezes de forma natural.", 10),
    HEAD_NOD("Balance a cabeca lentamente de cima para baixo duas vezes.", 15),
    HEAD_SHAKE("Balance a cabeca lentamente de um lado para o outro duas vezes.", 15);

    private final String instruction;
    private final int framesRequired;

    ChallengeType(String instruction, int framesRequired) {
        this.instruction = instruction;
        this.framesRequired = framesRequired;
    }

    public String getInstruction() {
        return instruction;
    }

    public int getFramesRequired() {
        return framesRequired;
    }
}