package com.faceid.util;

import org.bytedeco.opencv.opencv_core.Mat;

/**
 * Utilitários para Local Binary Pattern (LBP).
 *
 * LBP codifica a micro-textura de cada pixel comparando-o com seus 8 vizinhos:
 * o código é o número binário de 8 bits onde o i-ésimo bit = 1 se vizinho[i] >= centro.
 *
 * Usado em dois contextos:
 *   - TextureAnalyzer: mede entropia do histograma para anti-spoofing passivo.
 *   - FaceRecognitionService: compara histogramas de faces para verificação de identidade.
 */
public final class LBPUtils {

    private LBPUtils() {}

    /**
     * Computa o histograma LBP normalizado (probabilidades) de uma imagem em escala de cinza.
     *
     * @param grayFace Mat CV_8UC1 com o rosto.
     * @return Array de 256 doubles representando a distribuição de padrões LBP (soma = 1.0).
     */
    public static double[] computeHistogram(Mat grayFace) {
        int rows = grayFace.rows();
        int cols = grayFace.cols();
        double[] histogram = new double[256];

        byte[] raw = new byte[(int) grayFace.total()];
        grayFace.data().get(raw);

        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                int center = raw[r * cols + c] & 0xFF;
                int code = 0;
                // 8 vizinhos em sentido horário
                int[] n = {
                    raw[(r-1)*cols+(c-1)] & 0xFF, raw[(r-1)*cols+c] & 0xFF,
                    raw[(r-1)*cols+(c+1)] & 0xFF, raw[r    *cols+(c+1)] & 0xFF,
                    raw[(r+1)*cols+(c+1)] & 0xFF, raw[(r+1)*cols+c] & 0xFF,
                    raw[(r+1)*cols+(c-1)] & 0xFF, raw[r    *cols+(c-1)] & 0xFF
                };
                for (int i = 0; i < 8; i++) {
                    if (n[i] >= center) code |= (1 << i);
                }
                histogram[code]++;
            }
        }

        // Normaliza para distribuição de probabilidade
        double total = (double)(rows - 2) * (cols - 2);
        for (int i = 0; i < 256; i++) {
            histogram[i] /= total;
        }
        return histogram;
    }

    /**
     * Distância chi-quadrado entre dois histogramas LBP normalizados.
     *
     * Chi-quadrado = Σ((h1[i] - h2[i])² / (h1[i] + h2[i] + ε))
     * Quanto menor, mais similares são as texturas.
     *
     * Para histogramas normalizados, o valor máximo típico é ~2.0.
     * Mapeamos para similaridade: sim = exp(-chi²) → [0, 1], onde 1 = idêntico.
     *
     * @return Similaridade entre 0.0 (completamente diferente) e 1.0 (idêntico).
     */
    public static double chiSquareSimilarity(double[] h1, double[] h2) {
        double chiSq = 0.0;
        for (int i = 0; i < 256; i++) {
            double denom = h1[i] + h2[i];
            if (denom > 1e-10) {
                chiSq += Math.pow(h1[i] - h2[i], 2) / denom;
            }
        }
        // exp(-chi²) mapeia [0, ∞) → (0, 1] de forma suave
        return Math.exp(-chiSq);
    }

    /**
     * Entropia de Shannon do histograma LBP normalizado.
     * Mede a diversidade de padrões de textura (0 = uniforme, 1 = máximo).
     */
    public static double entropy(double[] normalizedHistogram) {
        double entropy = 0.0;
        for (double p : normalizedHistogram) {
            if (p > 0) entropy -= p * Math.log(p);
        }
        return Math.min(1.0, entropy / Math.log(256));
    }
}
