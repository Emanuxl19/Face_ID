package com.faceid.dto.ResponseDTO;

/**
 * Resultado da verificação facial com score de similaridade.
 *
 * @param verified    true se a similaridade atingiu o threshold configurado.
 * @param similarity  Score LBP chi-quadrado entre 0.0 (diferente) e 1.0 (idêntico).
 * @param message     Mensagem descritiva do resultado.
 */
public record FaceVerificationResultDTO(
        boolean verified,
        double similarity,
        String message
) {}
