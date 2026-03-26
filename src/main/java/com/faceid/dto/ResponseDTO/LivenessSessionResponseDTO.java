package com.faceid.dto.ResponseDTO;

import com.faceid.liveness.ChallengeType;
import com.faceid.liveness.LivenessSessionStatus;

import java.time.Instant;

/**
 * Estado atual de uma sessão de desafio de vivacidade.
 *
 * @param sessionId       ID UUID da sessão.
 * @param challengeType   Tipo de desafio solicitado.
 * @param instruction     Instrução a ser exibida para o usuário.
 * @param framesRequired  Quantidade mínima de frames para avaliação.
 * @param framesReceived  Quantidade de frames já enviados.
 * @param status          Status atual da sessão.
 * @param expiresAt       Timestamp de expiração.
 */
public record LivenessSessionResponseDTO(
        String sessionId,
        ChallengeType challengeType,
        String instruction,
        int framesRequired,
        int framesReceived,
        LivenessSessionStatus status,
        Instant expiresAt
) {}
