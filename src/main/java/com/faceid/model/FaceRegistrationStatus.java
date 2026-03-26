package com.faceid.model;

/**
 * Estado de uma sessão de cadastro facial multi-ângulo.
 */
public enum FaceRegistrationStatus {
    /** Sessão aberta, ainda coletando ângulos. */
    PENDING,
    /** Todos os ângulos obrigatórios foram capturados com sucesso. */
    COMPLETE,
    /** TTL expirado antes de completar o cadastro. */
    EXPIRED
}
