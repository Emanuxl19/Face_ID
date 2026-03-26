package com.faceid.liveness.analyzer;

import com.faceid.util.LBPUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

/**
 * Algoritmo: Entropia LBP (Local Binary Pattern).
 *
 * LBP codifica a micro-textura de cada pixel comparando-o com seus 8 vizinhos:
 * o código LBP é o número binário de 8 bits onde o i-ésimo bit = 1 se vizinho[i] >= centro.
 *
 * A entropia de Shannon do histograma dos 256 códigos mede a diversidade de texturas:
 *   - Alta entropia → distribuição uniforme de padrões → textura rica → face real.
 *   - Baixa entropia → poucos padrões dominantes → textura periódica → papel/tela.
 *
 * Fotos impressas têm padrões de meio-tom (halftone) e telas têm grade de pixels,
 * ambos com baixa entropia LBP.
 *
 * Score: entropia / ln(256), normalizado para 0–1.
 */
@Component
public class TextureAnalyzer extends PassiveAnalyzer {

    @Override
    public String algorithmName() {
        return "TEXTURE";
    }

    @Override
    public double analyze(Mat grayFace) {
        double[] histogram = LBPUtils.computeHistogram(grayFace);
        return LBPUtils.entropy(histogram);
    }

    @Override
    public double threshold() {
        return 0.55;
    }

    @Override
    public String lowScoreFlag() {
        return "SUSPICIOUS_TEXTURE";
    }

    @Override
    public double weight() {
        return 0.35;
    }
}
