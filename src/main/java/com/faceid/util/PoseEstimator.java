package com.faceid.util;

import com.faceid.model.FacePose;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

/**
 * Estimativa de pose facial a partir do bounding box do rosto na imagem completa.
 *
 * <p>Estratégia (heurística sem landmarks 3D):
 * <ol>
 *   <li>Calcula a posição relativa do centroide da face (relX, relY) na imagem.</li>
 *   <li>Calcula o aspect ratio do bounding box (largura/altura).
 *       Uma face girada lateralmente fica mais estreita (ratio < 0.72).</li>
 *   <li>Classifica a pose por precedência:
 *       UP/DOWN → lateral (LEFT/RIGHT por ratio + posição) → FRONT.</li>
 * </ol>
 *
 * <p>Limitação: funciona melhor quando o usuário é instruído a manter o rosto
 * centralizado e rotacioná-lo suavemente (como o Face ID da Apple).
 */
public final class PoseEstimator {

    // Thresholds de posição relativa (0.0–1.0 da dimensão da imagem)
    private static final double UP_THRESHOLD   = 0.38;
    private static final double DOWN_THRESHOLD = 0.62;
    private static final double LEFT_CENTER    = 0.42;
    private static final double RIGHT_CENTER   = 0.58;

    // Face estreita indica rotação lateral
    private static final double NARROW_ASPECT_RATIO = 0.72;

    private PoseEstimator() {}

    /**
     * Estima a pose a partir do bounding box {@code faceRect} dentro de {@code fullImage}.
     *
     * @param faceRect  Retângulo da face detectada na imagem completa.
     * @param fullImage Imagem completa em escala de cinza (usada apenas para dimensões).
     * @return Pose estimada.
     */
    public static FacePose estimatePose(Rect faceRect, Mat fullImage) {
        double imageWidth  = fullImage.cols();
        double imageHeight = fullImage.rows();

        double faceCenterX = faceRect.x() + faceRect.width()  / 2.0;
        double faceCenterY = faceRect.y() + faceRect.height() / 2.0;

        double relX = faceCenterX / imageWidth;
        double relY = faceCenterY / imageHeight;
        double aspectRatio = (double) faceRect.width() / faceRect.height();

        // Inclinação vertical tem precedência
        if (relY < UP_THRESHOLD)   return FacePose.UP;
        if (relY > DOWN_THRESHOLD) return FacePose.DOWN;

        // Face estreita + deslocamento lateral → rotação horizontal
        if (aspectRatio < NARROW_ASPECT_RATIO) {
            return relX < 0.50 ? FacePose.LEFT : FacePose.RIGHT;
        }

        // Deslocamento horizontal pronunciado mesmo com face larga
        if (relX < LEFT_CENTER)  return FacePose.LEFT;
        if (relX > RIGHT_CENTER) return FacePose.RIGHT;

        return FacePose.FRONT;
    }
}
