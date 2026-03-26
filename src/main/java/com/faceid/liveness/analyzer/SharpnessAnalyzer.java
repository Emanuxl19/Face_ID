package com.faceid.liveness.analyzer;

import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.Laplacian;

/**
 * Algoritmo: Variância Laplaciana (nitidez / blur detection).
 *
 * Aplica o operador Laplaciano (segunda derivada da imagem) e calcula a variância
 * da resposta. Imagens nítidas têm alta variância porque o Laplaciano amplifica
 * transições de intensidade (bordas). Imagens borradas — como uma foto de papel
 * fotografada ou uma tela capturada com câmera — têm baixa variância.
 *
 * Faixa empírica calibrada:
 *   - Foto de papel/tela: variância ≈ 20–80
 *   - Rosto real ao vivo:  variância ≈ 100–600+
 *
 * Score normalizado: min(variância / 400, 1.0)
 */
@Component
public class SharpnessAnalyzer extends PassiveAnalyzer {

    @Override
    public String algorithmName() {
        return "SHARPNESS";
    }

    @Override
    public double analyze(Mat grayFace) {
        Mat gray64f   = new Mat();
        Mat laplacian = new Mat();
        grayFace.convertTo(gray64f, CV_64F);
        Laplacian(gray64f, laplacian, CV_64F);

        Mat mean   = new Mat();
        Mat stddev = new Mat();
        meanStdDev(laplacian, mean, stddev);

        DoubleIndexer idx = stddev.createIndexer();
        double std = idx.get(0L);
        idx.close();

        return Math.min(1.0, (std * std) / 400.0);
    }

    @Override
    public double threshold() {
        return 0.25;
    }

    @Override
    public String lowScoreFlag() {
        return "LOW_SHARPNESS";
    }

    @Override
    public double weight() {
        return 0.40;
    }
}
