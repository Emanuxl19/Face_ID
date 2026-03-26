package com.faceid.util;

import com.faceid.model.FacePose;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a lógica de estimativa de pose sem carregar o Spring context.
 * Usa Mat e Rect sintéticos para simular diferentes posições faciais.
 *
 * Convenção de tamanho de imagem: 640×480 px (típico câmera frontal).
 * Face "normal" (FRONT): bounding box 160×160 centrado em (240, 160) — relX=0.5, relY=0.5, ratio=1.0.
 */
class PoseEstimatorTest {

    // Imagem de referência 640×480
    private static final int W = 640;
    private static final int H = 480;

    /**
     * Cria um Mat vazio com as dimensões especificadas (apenas cols/rows importam aqui).
     */
    private static Mat image(int cols, int rows) {
        return new Mat(rows, cols, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
    }

    /** Face centralizada com aspect ratio ~1.0 → FRONT */
    @Test
    void centeredFaceWithNormalRatioShouldBeFront() {
        // Centro: (320, 240), face 160×160 → relX=0.5, relY=0.5, ratio=1.0
        Rect face = new Rect(240, 160, 160, 160);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.FRONT);
    }

    /** Face estreita (ratio < 0.72) no lado esquerdo do frame → LEFT */
    @Test
    void narrowFaceOnLeftSideShouldBeLeft() {
        // Face 80×130 (ratio=0.615) centrada em x=180 → relX=0.28
        Rect face = new Rect(140, 175, 80, 130);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.LEFT);
    }

    /** Face estreita (ratio < 0.72) no lado direito do frame → RIGHT */
    @Test
    void narrowFaceOnRightSideShouldBeRight() {
        // Face 80×130 centrada em x=420 → relX=0.66
        Rect face = new Rect(380, 175, 80, 130);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.RIGHT);
    }

    /** Centroide vertical < 38% da altura → UP */
    @Test
    void faceHighInFrameShouldBeUp() {
        // Centro Y = 80 → relY = 80/480 = 0.167
        Rect face = new Rect(240, 0, 160, 160);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.UP);
    }

    /** Centroide vertical > 62% da altura → DOWN */
    @Test
    void faceLowInFrameShouldBeDown() {
        // Centro Y = 400 → relY = 400/480 = 0.833
        Rect face = new Rect(240, 320, 160, 160);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.DOWN);
    }

    /** Face deslocada para a esquerda sem estreitamento → LEFT por posição */
    @Test
    void faceCentroidFarLeftShouldBeLeft() {
        // relX = (100 + 80) / 640 = 0.28 → < 0.42, ratio = 160/160 = 1.0
        Rect face = new Rect(20, 160, 160, 160);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.LEFT);
    }

    /** Face deslocada para a direita sem estreitamento → RIGHT por posição */
    @Test
    void faceCentroidFarRightShouldBeRight() {
        // relX = (460 + 80) / 640 = 0.84 → > 0.58
        Rect face = new Rect(460, 160, 160, 160);
        FacePose pose = PoseEstimator.estimatePose(face, image(W, H));
        assertThat(pose).isEqualTo(FacePose.RIGHT);
    }
}
