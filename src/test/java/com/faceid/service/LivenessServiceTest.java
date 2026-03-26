package com.faceid.service;

import com.faceid.dto.ResponseDTO.LivenessResultDTO;
import com.faceid.dto.ResponseDTO.LivenessSessionResponseDTO;
import com.faceid.exception.BadRequestException;
import com.faceid.liveness.ChallengeType;
import com.faceid.liveness.LivenessSessionStatus;
import com.faceid.liveness.analyzer.GradientAnalyzer;
import com.faceid.liveness.analyzer.SharpnessAnalyzer;
import com.faceid.liveness.analyzer.TextureAnalyzer;
import com.faceid.model.FaceDetectionData;
import com.faceid.model.LivenessSession;
import com.faceid.repository.LivenessSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivenessServiceTest {

    @Mock private LivenessSessionRepository sessionRepository;
    @Mock private FaceRecognitionService    faceRecognitionService;

    private LivenessService livenessService;

    // Analyzers reais — os algoritmos OpenCV são testados end-to-end sem mocks
    private final SharpnessAnalyzer sharpnessAnalyzer = new SharpnessAnalyzer();
    private final TextureAnalyzer   textureAnalyzer   = new TextureAnalyzer();
    private final GradientAnalyzer  gradientAnalyzer  = new GradientAnalyzer();

    @BeforeEach
    void setUp() {
        // Constrói o serviço injetando dependências manualmente — permite misturar
        // mocks (repositório, faceService) com dependências reais (analyzers, executor)
        livenessService = new LivenessService(
                sessionRepository,
                faceRecognitionService,
                new ObjectMapper(),
                List.of(sharpnessAnalyzer, textureAnalyzer, gradientAnalyzer),
                Executors.newFixedThreadPool(3)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SharpnessAnalyzer — Variância Laplaciana
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("SharpnessAnalyzer: imagem uniforme (sem bordas) deve ter score baixo")
    void sharpness_uniformImage_returnsLowScore() {
        Mat gray = Mat.zeros(100, 100, CV_8UC1).asMat();
        assertThat(sharpnessAnalyzer.analyze(gray)).isLessThan(0.05);
    }

    @Test
    @DisplayName("SharpnessAnalyzer: xadrez fino (muitas bordas) deve ter score alto")
    void sharpness_checkerboard_returnsHighScore() {
        assertThat(sharpnessAnalyzer.analyze(buildCheckerboard(100, 100, 5))).isGreaterThan(0.30);
    }

    @Test
    @DisplayName("SharpnessAnalyzer: score deve estar no intervalo [0.0, 1.0]")
    void sharpness_scoreBoundedBetween0and1() {
        double score = sharpnessAnalyzer.analyze(buildRandomNoise(100, 100));
        assertThat(score).isBetween(0.0, 1.0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TextureAnalyzer — Entropia LBP
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TextureAnalyzer: imagem uniforme deve ter entropia LBP baixa")
    void texture_uniformImage_returnsLowEntropy() {
        Mat gray = new Mat(100, 100, CV_8UC1, new org.bytedeco.opencv.opencv_core.Scalar(128));
        assertThat(textureAnalyzer.analyze(gray)).isLessThan(0.05);
    }

    @Test
    @DisplayName("TextureAnalyzer: ruído aleatório deve ter entropia LBP máxima")
    void texture_randomNoise_returnsHighEntropy() {
        assertThat(textureAnalyzer.analyze(buildRandomNoise(100, 100))).isGreaterThan(0.80);
    }

    @Test
    @DisplayName("TextureAnalyzer: score deve estar no intervalo [0.0, 1.0]")
    void texture_scoreBoundedBetween0and1() {
        double score = textureAnalyzer.analyze(buildCheckerboard(100, 100, 3));
        assertThat(score).isBetween(0.0, 1.0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GradientAnalyzer — Magnitude Sobel
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GradientAnalyzer: imagem uniforme deve ter gradiente próximo de zero")
    void gradient_uniformImage_returnsLowScore() {
        Mat gray = new Mat(100, 100, CV_8UC1, new org.bytedeco.opencv.opencv_core.Scalar(200));
        assertThat(gradientAnalyzer.analyze(gray)).isLessThan(0.05);
    }

    @Test
    @DisplayName("GradientAnalyzer: xadrez deve ter gradiente alto")
    void gradient_checkerboard_returnsHighScore() {
        assertThat(gradientAnalyzer.analyze(buildCheckerboard(100, 100, 5))).isGreaterThan(0.25);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LivenessService — análise passiva com ExecutorService + Stream API
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("analyzePassive: imagem uniforme (simula foto de papel) deve retornar SPOOF")
    void analyzePassive_flatImage_returnsSpoof() {
        Mat flat = new Mat(100, 100, CV_8UC1, new org.bytedeco.opencv.opencv_core.Scalar(128));
        FaceDetectionData detection = new FaceDetectionData(flat, new Rect(0, 0, 100, 100), flat);

        doNothing().when(faceRecognitionService).validateImageBytes(any());
        when(faceRecognitionService.detectFaceWithData(any())).thenReturn(detection);

        LivenessResultDTO result = livenessService.analyzePassive(new byte[]{1, 2, 3});

        assertThat(result.verdict()).isEqualTo("SPOOF");
        assertThat(result.live()).isFalse();
        assertThat(result.flags()).contains("LOW_SHARPNESS", "SUSPICIOUS_TEXTURE", "LOW_GRADIENT");
    }

    @Test
    @DisplayName("analyzePassive: textura rica (simula face real) deve retornar LIVE")
    void analyzePassive_richTexture_returnsLive() {
        Mat rich = buildRichTexture(100, 100);
        FaceDetectionData detection = new FaceDetectionData(rich, new Rect(0, 0, 100, 100), rich);

        doNothing().when(faceRecognitionService).validateImageBytes(any());
        when(faceRecognitionService.detectFaceWithData(any())).thenReturn(detection);

        LivenessResultDTO result = livenessService.analyzePassive(new byte[]{1, 2, 3});

        assertThat(result.live()).isTrue();
        assertThat(result.overallScore()).isGreaterThanOrEqualTo(0.55);
    }

    @Test
    @DisplayName("analyzePassive: scores individuais devem somar para o overallScore ponderado")
    void analyzePassive_overallScoreIsWeightedAverage() {
        Mat rich = buildRichTexture(100, 100);
        FaceDetectionData detection = new FaceDetectionData(rich, new Rect(0, 0, 100, 100), rich);

        doNothing().when(faceRecognitionService).validateImageBytes(any());
        when(faceRecognitionService.detectFaceWithData(any())).thenReturn(detection);

        LivenessResultDTO result = livenessService.analyzePassive(new byte[]{1, 2, 3});

        // Verifica que o overall é a combinação ponderada dos três scores
        double expected = (result.sharpnessScore() * 0.40)
                        + (result.textureScore()   * 0.35)
                        + (result.gradientScore()  * 0.25);
        assertThat(result.overallScore()).isCloseTo(expected, within(0.01));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LivenessService — gestão de sessões
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createSession deve retornar sessão PENDING com instruções do desafio")
    void createSession_returnsPendingWithInstruction() {
        LivenessSession persisted = buildSession(ChallengeType.BLINK, LivenessSessionStatus.PENDING, 60);
        when(sessionRepository.save(any())).thenReturn(persisted);

        LivenessSessionResponseDTO response = livenessService.createSession(1L, ChallengeType.BLINK);

        assertThat(response.status()).isEqualTo(LivenessSessionStatus.PENDING);
        assertThat(response.challengeType()).isEqualTo(ChallengeType.BLINK);
        assertThat(response.framesRequired()).isEqualTo(ChallengeType.BLINK.getFramesRequired());
        assertThat(response.framesReceived()).isZero();
        assertThat(response.instruction()).isNotBlank();
    }

    @Test
    @DisplayName("addFrame deve incrementar contagem e atualizar status para COLLECTING")
    void addFrame_incrementsFrameCount() {
        UUID id = UUID.randomUUID();
        LivenessSession session = buildSession(ChallengeType.HEAD_SHAKE, LivenessSessionStatus.PENDING, 60);
        session.setId(id);

        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenReturn(session);
        doNothing().when(faceRecognitionService).validateImageBytes(any());

        livenessService.addFrame(id, new byte[]{1});
        LivenessSessionResponseDTO response = livenessService.addFrame(id, new byte[]{2});

        assertThat(response.framesReceived()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(LivenessSessionStatus.COLLECTING);
    }

    @Test
    @DisplayName("addFrame em sessão já completada deve lançar BadRequestException")
    void addFrame_completedSession_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        LivenessSession session = buildSession(ChallengeType.PASSIVE, LivenessSessionStatus.COMPLETED, 60);
        session.setId(id);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> livenessService.addFrame(id, new byte[]{1}))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("concluida");
    }

    @Test
    @DisplayName("evaluateSession com frames insuficientes deve lançar BadRequestException")
    void evaluateSession_insufficientFrames_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        LivenessSession session = buildSession(ChallengeType.BLINK, LivenessSessionStatus.COLLECTING, 60);
        session.setId(id);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> livenessService.evaluateSession(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("insuficientes");
    }

    @Test
    @DisplayName("evaluateSession com sessão expirada deve lançar BadRequestException")
    void evaluateSession_expiredSession_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        LivenessSession session = buildSession(ChallengeType.PASSIVE, LivenessSessionStatus.PENDING, -10);
        session.setId(id);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> livenessService.evaluateSession(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expirada");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers para Mats sintéticos
    // ═══════════════════════════════════════════════════════════════════════

    /** Xadrez: alta variância Laplaciana + alto gradiente Sobel. */
    private Mat buildCheckerboard(int rows, int cols, int blockSize) {
        Mat mat = new Mat(rows, cols, CV_8UC1);
        byte[] data = new byte[rows * cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean white = ((r / blockSize) + (c / blockSize)) % 2 == 0;
                data[r * cols + c] = white ? (byte) 255 : 0;
            }
        }
        mat.data().put(data);
        return mat;
    }

    /** Ruído aleatório: máxima entropia LBP (256 padrões uniformemente distribuídos). */
    private Mat buildRandomNoise(int rows, int cols) {
        Mat mat = new Mat(rows, cols, CV_8UC1);
        byte[] data = new byte[rows * cols];
        new java.util.Random(42).nextBytes(data);
        mat.data().put(data);
        return mat;
    }

    /**
     * Textura rica: mistura xadrez fino (60%) + ruído (40%).
     * Resulta em alta nitidez, alta entropia LBP e alto gradiente — simula face real.
     */
    private Mat buildRichTexture(int rows, int cols) {
        Mat checker = buildCheckerboard(rows, cols, 3);
        Mat noise   = buildRandomNoise(rows, cols);
        Mat result  = new Mat();
        org.bytedeco.opencv.global.opencv_core.addWeighted(checker, 0.6, noise, 0.4, 0, result);
        return result;
    }

    private LivenessSession buildSession(ChallengeType type, LivenessSessionStatus status, int ttlSeconds) {
        LivenessSession session = new LivenessSession();
        session.setId(UUID.randomUUID());
        session.setUserId(1L);
        session.setChallengeType(type);
        session.setStatus(status);
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        return session;
    }
}
