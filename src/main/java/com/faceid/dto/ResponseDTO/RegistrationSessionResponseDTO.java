package com.faceid.dto.ResponseDTO;

import com.faceid.model.FacePose;
import com.faceid.model.FaceRegistrationStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Retorna o estado atual de uma sessão de cadastro facial multi-ângulo.
 *
 * @param sessionId       Identificador da sessão (UUID).
 * @param userId          Usuário associado à sessão.
 * @param status          Estado atual: PENDING, COMPLETE ou EXPIRED.
 * @param requiredPoses   Conjunto de poses obrigatórias para concluir o cadastro.
 * @param collectedPoses  Poses já capturadas com sucesso.
 * @param remainingPoses  Poses que ainda faltam.
 * @param expiresAt       Momento em que a sessão expira automaticamente.
 */
public record RegistrationSessionResponseDTO(
        String sessionId,
        Long userId,
        FaceRegistrationStatus status,
        Set<FacePose> requiredPoses,
        Set<FacePose> collectedPoses,
        Set<FacePose> remainingPoses,
        Instant expiresAt
) {}
