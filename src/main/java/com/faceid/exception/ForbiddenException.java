package com.faceid.exception;

/**
 * Lançada quando um usuário autenticado tenta acessar um recurso que não lhe pertence.
 * Resulta em HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
