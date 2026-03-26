package com.faceid.model;

/**
 * Eventos auditáveis no sistema.
 * Cada evento é gravado em {@code audit_logs} com IP, userId e detalhes.
 */
public enum AuditEvent {
    // Autenticação
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGIN_BLOCKED,       // IP bloqueado por excesso de tentativas
    LOGOUT,              // Token invalidado explicitamente

    // Dados de usuário
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    // Face
    FACE_REGISTERED,
    FACE_MULTI_ANGLE_SESSION_STARTED,
    FACE_MULTI_ANGLE_COMPLETED,
    FACE_VERIFICATION_SUCCESS,
    FACE_VERIFICATION_FAILURE,

    // Segurança
    ACCESS_DENIED        // 403 — tentativa de acesso a recurso de outro usuário
}
