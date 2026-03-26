package com.faceid.liveness.analyzer;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.stereotype.Component;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Algoritmo: Magnitude do Gradiente Sobel (riqueza de bordas).
 *
 * Aplica filtros Sobel horizontal (Gx) e vertical (Gy) e calcula a magnitude
 * resultante: magnitude = sqrt(Gx² + Gy²). A média da magnitude no rosto mede
 * a quantidade e intensidade de bordas presentes.
 *
 * Faces reais têm bordas complexas (poros, pelos, rugas, olhos, boca).
 * Imagens impressas ou exibidas em tela têm bordas mais suaves e uniformes,
 * resultando em magnitude média mais baixa.
 *
 * Score: mean(magnitude) / 40, normalizado para 0–1.
 */
@Component
public class GradientAnalyzer extends PassiveAnalyzer {

    @Override
    public String algorithmName() {
        return "GRADIENT";
    }

    @Override
    public double analyze(Mat grayFace) {
        Mat gray64f = new Mat();
        grayFace.convertTo(gray64f, CV_64F);

        Mat gradX = new Mat();
        Mat gradY = new Mat();
        Mat mag   = new Mat();

        Sobel(gray64f, gradX, CV_64F, 1, 0, 3, 1.0, 0.0, BORDER_DEFAULT);
        Sobel(gray64f, gradY, CV_64F, 0, 1, 3, 1.0, 0.0, BORDER_DEFAULT);
        magnitude(gradX, gradY, mag);

        Scalar meanVal = mean(mag);
        return Math.min(1.0, meanVal.get(0) / 40.0);
    }

    @Override
    public double threshold() {
        return 0.20;
    }

    @Override
    public String lowScoreFlag() {
        return "LOW_GRADIENT";
    }

    @Override
    public double weight() {
        return 0.25;
    }
}
