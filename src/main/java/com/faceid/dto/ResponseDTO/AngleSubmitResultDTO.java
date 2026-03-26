package com.faceid.dto.ResponseDTO;

import com.faceid.model.FacePose;
import com.faceid.model.FaceRegistrationStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Resultado do envio de um frame de ângulo durante o cadastro multi-ângulo.
 *
 * @param sessionId           ID da sessão.
 * @param poseDetected        Pose identificada neste frame.
 * @param poseAlreadyCollected {@code true} se este ângulo já foi capturado antes (frame ignorado).
 * @param collectedPoses      Poses acumuladas até agora.
 * @param remainingPoses      Poses que ainda precisam ser capturadas.
 * @param status              Estado atualizado da sessão após este frame.
 * @param expiresAt           Expiração da sessão.
 * @param instruction         Instrução para o próximo ângulo a ser capturado (ou null se concluído).
 */
public record AngleSubmitResultDTO(
        String sessionId,
        FacePose poseDetected,
        boolean poseAlreadyCollected,
        Set<FacePose> collectedPoses,
        Set<FacePose> remainingPoses,
        FaceRegistrationStatus status,
        Instant expiresAt,
        String instruction
) {}
