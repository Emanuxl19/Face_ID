package com.faceid.liveness;

/**
 * Estados possíveis de uma sessão de desafio de vivacidade.
 */
public enum LivenessSessionStatus {
    PENDING,     // Sessão criada, aguardando primeiro frame
    COLLECTING,  // Recebendo frames do cliente
    COMPLETED,   // Avaliação concluída
    EXPIRED,     // TTL expirou antes da avaliação
    FAILED       // Erro durante a avaliação
}
