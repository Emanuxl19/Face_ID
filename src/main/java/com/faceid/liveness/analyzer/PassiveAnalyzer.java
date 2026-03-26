package com.faceid.liveness.analyzer;

import org.bytedeco.opencv.opencv_core.Mat;

/**
 * Contrato para algoritmos de análise passiva de vivacidade (single-frame).
 *
 * Cada subclasse implementa um algoritmo de visão computacional independente.
 * O LivenessService coleta todos os beans desta hierarquia e os executa em paralelo
 * via ExecutorService + CompletableFuture, combinando os scores com os pesos definidos.
 *
 * Para adicionar um novo algoritmo basta criar uma subclasse anotada com @Component —
 * o Spring a injetará automaticamente na lista do LivenessService.
 */
public abstract class PassiveAnalyzer {

    /**
     * Identificador único do algoritmo. Usado como chave no mapa de scores do DTO.
     * Exemplos: "SHARPNESS", "TEXTURE", "GRADIENT"
     */
    public abstract String algorithmName();

    /**
     * Executa a análise no rosto recortado em escala de cinza (100×100).
     *
     * @param grayFace Mat CV_8UC1 com rosto detectado, redimensionado para 100×100.
     * @return Score entre 0.0 (forte indício de spoof) e 1.0 (forte indício de face real).
     */
    public abstract double analyze(Mat grayFace);

    /**
     * Score mínimo abaixo do qual a imagem é considerada suspeita para este algoritmo.
     * Quando score < threshold(), a flag {@link #lowScoreFlag()} é adicionada ao resultado.
     */
    public abstract double threshold();

    /** Flag descritiva adicionada ao LivenessResultDTO quando score < threshold(). */
    public abstract String lowScoreFlag();

    /**
     * Peso deste algoritmo na pontuação geral ponderada.
     * A soma dos pesos de todos os analisadores deve ser normalizada pelo LivenessService.
     */
    public abstract double weight();
}
