package com.faceid.dto.ResponseDTO;

import java.util.List;

/**
 * Resultado da análise de vivacidade (anti-spoofing).
 *
 * @param live           true se o rosto for classificado como real.
 * @param overallScore   Pontuação geral ponderada entre 0.0 (spoof) e 1.0 (real).
 * @param sharpnessScore Pontuação de nitidez via variância Laplaciana (0–1).
 * @param textureScore   Pontuação de textura via entropia LBP (0–1).
 * @param gradientScore  Pontuação de gradiente via magnitude Sobel (0–1).
 * @param motionScore    Pontuação de movimento (fluxo óptico / centroide) para desafios ativos (0–1).
 * @param verdict        "LIVE", "UNCERTAIN" ou "SPOOF".
 * @param flags          Lista de flags descritivas sobre o resultado.
 */
public record LivenessResultDTO(
        boolean live,
        double overallScore,
        double sharpnessScore,
        double textureScore,
        double gradientScore,
        double motionScore,
        String verdict,
        List<String> flags
) {}
