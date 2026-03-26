package com.faceid.service;

import com.faceid.dto.ResponseDTO.LivenessResultDTO;
import com.faceid.dto.ResponseDTO.LivenessSessionResponseDTO;
import com.faceid.exception.BadRequestException;
import com.faceid.exception.ResourceNotFoundException;
import com.faceid.liveness.ChallengeType;
import com.faceid.liveness.LivenessSessionStatus;
import com.faceid.liveness.analyzer.PassiveAnalyzer;
import com.faceid.model.FaceDetectionData;
import com.faceid.model.LivenessSession;
import com.faceid.repository.LivenessSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bytedeco.opencv.opencv_core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_video.calcOpticalFlowFarneback;

/**
 * Liveness-Lab: serviço de detecção de vivacidade (anti-spoofing).
 *
 * ── ANÁLISE PASSIVA (frame único) ─────────────────────────────────────────
 * Os algoritmos passivos (Laplacian, LBP, Sobel) são executados em PARALELO via
 * ExecutorService + CompletableFuture e são combinados com pesos via Stream API.
 * Para adicionar um novo algoritmo, basta criar um @Component que estenda PassiveAnalyzer.
 *
 * ── ANÁLISE ATIVA (sessão multi-frame) ────────────────────────────────────
 * BLINK       — série temporal de brilho ocular (DoubleSummaryStatistics).
 * HEAD_NOD    — rastreamento de centroide Y + fluxo óptico Farneback.
 * HEAD_SHAKE  — rastreamento de centroide X + fluxo óptico Farneback.
 *
 * ── HIBERNATE ─────────────────────────────────────────────────────────────
 * @Version em LivenessSession previne que addFrame() paralelos corrompam o estado.
 */
@Service
public class LivenessService {

    private static final Logger log = LoggerFactory.getLogger(LivenessService.class);

    // ── Thresholds de scoring ─────────────────────────────────────────────
    private static final double LIVENESS_THRESHOLD  = 0.55;
    private static final double UNCERTAIN_THRESHOLD = 0.40;

    // ── Parâmetros de movimento ───────────────────────────────────────────
    private static final double MIN_HEAD_DISPLACEMENT = 15.0;
    private static final double MAX_HEAD_DISPLACEMENT = 60.0;

    // ── Parâmetros de sessão ──────────────────────────────────────────────
    private static final int SESSION_TTL_SECONDS     = 45;
    private static final int MAX_FRAMES_PER_SESSION  = 30;

    // ── Dependências ──────────────────────────────────────────────────────
    private final LivenessSessionRepository sessionRepository;
    private final FaceRecognitionService    faceRecognitionService;
    private final ObjectMapper              objectMapper;
    private final List<PassiveAnalyzer>     passiveAnalyzers;  // injetados pelo Spring (todos os @Component)
    private final ExecutorService           analysisExecutor;  // pool dedicado (3 threads)

    /**
     * Buffer de frames em memória: sessionId → lista sincronizada de bytes.
     * Frames são ephemeral (TTL 45s) — não faz sentido persistir no banco.
     */
    private final ConcurrentHashMap<UUID, List<byte[]>> sessionFrames = new ConcurrentHashMap<>();

    public LivenessService(LivenessSessionRepository sessionRepository,
                           FaceRecognitionService faceRecognitionService,
                           ObjectMapper objectMapper,
                           List<PassiveAnalyzer> passiveAnalyzers,
                           @Qualifier("livenessExecutor") ExecutorService analysisExecutor) {
        this.sessionRepository   = sessionRepository;
        this.faceRecognitionService = faceRecognitionService;
        this.objectMapper        = objectMapper;
        this.passiveAnalyzers    = passiveAnalyzers;
        this.analysisExecutor    = analysisExecutor;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANÁLISE PASSIVA — frame único, sem sessão
    // ═══════════════════════════════════════════════════════════════════════

    public LivenessResultDTO analyzePassive(byte[] imageData) {
        faceRecognitionService.validateImageBytes(imageData);
        FaceDetectionData detection = faceRecognitionService.detectFaceWithData(imageData);
        return computePassiveScore(detection.croppedFace());
    }

    /**
     * Despacha os algoritmos passivos em paralelo e combina os resultados.
     *
     * Fluxo:
     *   1. CompletableFuture.supplyAsync() — cada algoritmo em thread dedicado.
     *   2. Stream.map(join) — aguarda todos e coleta os resultados.
     *   3. Streams — calcula score ponderado e filtra flags.
     *   4. Collectors.toMap() — indexa scores por nome para o DTO.
     *
     * Segurança de threads: os algoritmos apenas LEEM o Mat (sem escrita),
     * tornando o acesso concorrente seguro para memória nativa do bytedeco.
     */
    private LivenessResultDTO computePassiveScore(Mat grayFace) {
        // 1. Executa todos os analisadores em paralelo
        List<CompletableFuture<AnalyzerResult>> futures = passiveAnalyzers.stream()
                .map(analyzer -> CompletableFuture.supplyAsync(() -> {
                    try {
                        double score = analyzer.analyze(grayFace);
                        return new AnalyzerResult(
                                analyzer.algorithmName(), score,
                                analyzer.weight(), analyzer.threshold(),
                                analyzer.lowScoreFlag());
                    } catch (Exception e) {
                        log.warn("Analyzer [{}] falhou: {}", analyzer.algorithmName(), e.getMessage());
                        return new AnalyzerResult(
                                analyzer.algorithmName(), 0.0,
                                analyzer.weight(), analyzer.threshold(),
                                analyzer.lowScoreFlag());
                    }
                }, analysisExecutor))
                .toList();

        // 2. Aguarda todos e coleta os resultados
        List<AnalyzerResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // 3. Score ponderado: Σ(score_i × weight_i) / Σ(weight_i)
        double totalWeight = passiveAnalyzers.stream().mapToDouble(PassiveAnalyzer::weight).sum();
        double overall = results.stream()
                .mapToDouble(r -> r.score() * r.weight())
                .sum() / totalWeight;

        // 4. Flags para scores abaixo do threshold de cada algoritmo
        List<String> flags = results.stream()
                .filter(r -> r.score() < r.threshold())
                .map(AnalyzerResult::flag)
                .collect(Collectors.toCollection(ArrayList::new));

        // 5. Mapa nome → score para compor o DTO sem depender de índice
        Map<String, Double> scoreMap = results.stream()
                .collect(Collectors.toMap(AnalyzerResult::name, AnalyzerResult::score));

        return buildResult(
                overall,
                scoreMap.getOrDefault("SHARPNESS", 0.0),
                scoreMap.getOrDefault("TEXTURE", 0.0),
                scoreMap.getOrDefault("GRADIENT", 0.0),
                0.0, flags);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GESTÃO DE SESSÕES
    // ═══════════════════════════════════════════════════════════════════════

    public LivenessSessionResponseDTO createSession(Long userId, ChallengeType challengeType) {
        LivenessSession session = new LivenessSession();
        session.setUserId(userId);
        session.setChallengeType(challengeType);
        session.setStatus(LivenessSessionStatus.PENDING);
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(SESSION_TTL_SECONDS));

        LivenessSession saved = sessionRepository.save(session);
        sessionFrames.put(saved.getId(), Collections.synchronizedList(new ArrayList<>()));
        return toSessionResponse(saved, 0);
    }

    /**
     * Adiciona um frame à sessão.
     * O @Version em LivenessSession garante que salvamentos concorrentes não
     * corrompam o status — o Hibernate lança OptimisticLockException se dois
     * threads modificarem a mesma sessão simultaneamente.
     */
    public LivenessSessionResponseDTO addFrame(UUID sessionId, byte[] frameData) {
        LivenessSession session = getSessionOrThrow(sessionId);
        checkNotExpired(session);

        if (session.getStatus() == LivenessSessionStatus.COMPLETED) {
            throw new BadRequestException("Sessao ja foi concluida. Crie uma nova sessao.");
        }

        faceRecognitionService.validateImageBytes(frameData);

        List<byte[]> frames = sessionFrames.computeIfAbsent(
                sessionId, k -> Collections.synchronizedList(new ArrayList<>()));

        if (frames.size() >= MAX_FRAMES_PER_SESSION) {
            throw new BadRequestException("Numero maximo de frames atingido (" + MAX_FRAMES_PER_SESSION + ").");
        }

        frames.add(frameData);
        session.setStatus(LivenessSessionStatus.COLLECTING);
        sessionRepository.save(session);

        return toSessionResponse(session, frames.size());
    }

    public LivenessResultDTO evaluateSession(UUID sessionId) {
        LivenessSession session = getSessionOrThrow(sessionId);
        checkNotExpired(session);

        List<byte[]> frames = sessionFrames.getOrDefault(sessionId, List.of());
        int required = session.getChallengeType().getFramesRequired();

        if (frames.size() < required) {
            throw new BadRequestException(
                    "Frames insuficientes para avaliacao. Necessario: " + required +
                    ", recebido: " + frames.size() + ".");
        }

        LivenessResultDTO result = switch (session.getChallengeType()) {
            case PASSIVE    -> analyzePassive(frames.get(0));
            case BLINK      -> analyzeBlinkChallenge(frames);
            case HEAD_NOD   -> analyzeMovementChallenge(frames, false);
            case HEAD_SHAKE -> analyzeMovementChallenge(frames, true);
        };

        persistResult(session, result);
        sessionFrames.remove(sessionId);
        return result;
    }

    public LivenessSessionResponseDTO getSession(UUID sessionId) {
        LivenessSession session = getSessionOrThrow(sessionId);
        int framesReceived = sessionFrames.getOrDefault(sessionId, List.of()).size();
        return toSessionResponse(session, framesReceived);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DESAFIO: BLINK
    // ═══════════════════════════════════════════════════════════════════════

    private LivenessResultDTO analyzeBlinkChallenge(List<byte[]> frames) {
        List<String> flags = new ArrayList<>();

        // Stream API: mapeia cada frame para o brilho ocular (ou -1 se face não detectada)
        List<Double> brightnessSequence = frames.stream()
                .map(frame -> {
                    try {
                        FaceDetectionData det = faceRecognitionService.detectFaceWithData(frame);
                        return computeEyeRegionBrightness(det.croppedFace());
                    } catch (Exception e) {
                        return -1.0;
                    }
                })
                .toList();

        List<Double> validSeq = brightnessSequence.stream()
                .filter(b -> b >= 0)
                .toList();

        if (validSeq.size() < 3) {
            flags.add("INSUFFICIENT_FACE_DETECTIONS");
            return buildResult(0.1, 0.0, 0.0, 0.0, 0.0, flags);
        }

        boolean blinkDetected = detectBlinkInSequence(validSeq);
        flags.add(blinkDetected ? "BLINK_DETECTED" : "NO_BLINK_DETECTED");

        LivenessResultDTO passive = analyzePassive(frames.get(frames.size() / 2));
        flags.addAll(passive.flags());

        double motionScore = blinkDetected ? 1.0 : 0.0;
        double overall     = (passive.overallScore() * 0.50) + (motionScore * 0.50);

        return new LivenessResultDTO(
                overall >= LIVENESS_THRESHOLD, round(overall),
                passive.sharpnessScore(), passive.textureScore(), passive.gradientScore(),
                round(motionScore), computeVerdict(overall), flags);
    }

    /**
     * Detecta piscada usando DoubleSummaryStatistics para calcular a média da sequência.
     * Uma piscada = brilho cai abaixo de 75% da média, depois se recupera.
     */
    private boolean detectBlinkInSequence(List<Double> brightness) {
        // DoubleSummaryStatistics: count, sum, min, max, average em uma única passagem
        DoubleSummaryStatistics stats = brightness.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        double blinkThreshold = stats.getAverage() * 0.75;
        boolean inDip = false;
        int blinkCount = 0;

        for (double b : brightness) {
            if (!inDip && b < blinkThreshold)  { inDip = true; }
            else if (inDip && b >= blinkThreshold) { inDip = false; blinkCount++; }
        }
        return blinkCount >= 1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DESAFIO: HEAD_NOD / HEAD_SHAKE
    // ═══════════════════════════════════════════════════════════════════════

    private LivenessResultDTO analyzeMovementChallenge(List<byte[]> frames, boolean horizontal) {
        List<String> flags = new ArrayList<>();

        // Stream API com flatMap: frames com detecção falha produzem Stream.empty()
        List<double[]> centroids = frames.stream()
                .flatMap(frame -> {
                    try {
                        FaceDetectionData det = faceRecognitionService.detectFaceWithData(frame);
                        Rect r = det.faceRect();
                        return Stream.of(new double[]{
                                r.x() + r.width()  / 2.0,
                                r.y() + r.height() / 2.0
                        });
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                })
                .toList();

        if (centroids.size() < 5) {
            flags.add("INSUFFICIENT_FACE_DETECTIONS");
            return buildResult(0.1, 0.0, 0.0, 0.0, 0.0, flags);
        }

        double[] origin = centroids.get(0);
        double maxDisplacement = centroids.stream()
                .mapToDouble(c -> horizontal ? Math.abs(c[0] - origin[0]) : Math.abs(c[1] - origin[1]))
                .max().orElse(0.0);

        double avgFlow = computeAverageOpticalFlow(frames);

        boolean movementDetected = maxDisplacement > MIN_HEAD_DISPLACEMENT;
        flags.add(horizontal
                ? (movementDetected ? "HEAD_SHAKE_DETECTED" : "NO_HEAD_SHAKE_DETECTED")
                : (movementDetected ? "HEAD_NOD_DETECTED"   : "NO_HEAD_NOD_DETECTED"));

        LivenessResultDTO passive = analyzePassive(frames.get(frames.size() / 2));
        flags.addAll(passive.flags());

        double centroidScore = Math.min(1.0, maxDisplacement / MAX_HEAD_DISPLACEMENT);
        double flowScore     = Math.min(1.0, avgFlow / 5.0);
        double motionScore   = (centroidScore * 0.6) + (flowScore * 0.4);
        double overall       = (passive.overallScore() * 0.45) + (motionScore * 0.55);

        return new LivenessResultDTO(
                overall >= LIVENESS_THRESHOLD, round(overall),
                passive.sharpnessScore(), passive.textureScore(), passive.gradientScore(),
                round(motionScore), computeVerdict(overall), flags);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FLUXO ÓPTICO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fluxo óptico denso (Farneback) entre dois frames consecutivos.
     * Retorna a magnitude média do campo vetorial de deslocamentos pixel a pixel.
     */
    private double computeOpticalFlowMagnitude(Mat prevGray, Mat currGray) {
        Mat flow = new Mat();
        calcOpticalFlowFarneback(prevGray, currGray, flow, 0.5, 3, 15, 3, 5, 1.2, 0);

        MatVector channels = new MatVector(2);
        split(flow, channels);

        Mat mag = new Mat();
        magnitude(channels.get(0), channels.get(1), mag);

        return mean(mag).get(0);
    }

    /**
     * Calcula a magnitude média de fluxo óptico entre todos os pares consecutivos.
     *
     * IntStream.range(1, n) gera índices 1..n-1, permitindo acesso ao par (i-1, i)
     * sem loop imperativo e com tratamento funcional de exceções.
     */
    private double computeAverageOpticalFlow(List<byte[]> frames) {
        return IntStream.range(1, frames.size())
                .mapToDouble(i -> {
                    try {
                        return computeOpticalFlowMagnitude(
                                decodeGray(frames.get(i - 1)),
                                decodeGray(frames.get(i)));
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .filter(f -> f > 0)
                .average()
                .orElse(0.0);
    }

    /**
     * Brilho médio da região ocular (25%–50% superior do rosto).
     * Fechamento de pálpebra reduz o brilho médio desta faixa.
     */
    private double computeEyeRegionBrightness(Mat grayFace) {
        int rows   = grayFace.rows();
        int cols   = grayFace.cols();
        int eyeTop = (int)(rows * 0.25);
        int eyeBot = (int)(rows * 0.50);

        Mat eyeRegion = new Mat(grayFace, new Rect(0, eyeTop, cols, eyeBot - eyeTop));
        return mean(eyeRegion).get(0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LIMPEZA AGENDADA
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredSessions() {
        List<LivenessSession> stale = sessionRepository
                .findByExpiresAtBeforeAndStatusNot(Instant.now(), LivenessSessionStatus.COMPLETED);

        if (stale.isEmpty()) return;

        // Stream API: atualiza status e remove frames em uma única operação
        stale.forEach(s -> {
            s.setStatus(LivenessSessionStatus.EXPIRED);
            sessionFrames.remove(s.getId());
        });

        sessionRepository.saveAll(stale);
        log.info("{} sessoes de vivacidade marcadas como EXPIRED.", stale.size());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Mat decodeGray(byte[] imageData) {
        Mat src  = imdecode(new Mat(imageData), IMREAD_COLOR);
        Mat gray = new Mat();
        cvtColor(src, gray, COLOR_BGR2GRAY);
        return gray;
    }

    private LivenessResultDTO buildResult(double overall, double sharpness, double texture,
                                          double gradient, double motion, List<String> flags) {
        return new LivenessResultDTO(
                overall >= LIVENESS_THRESHOLD,
                round(overall), round(sharpness), round(texture),
                round(gradient), round(motion),
                computeVerdict(overall), flags);
    }

    private String computeVerdict(double score) {
        if (score >= LIVENESS_THRESHOLD)  return "LIVE";
        if (score >= UNCERTAIN_THRESHOLD) return "UNCERTAIN";
        return "SPOOF";
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private LivenessSession getSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessao de vivacidade nao encontrada: " + sessionId));
    }

    private void checkNotExpired(LivenessSession session) {
        if (session.getExpiresAt().isBefore(Instant.now())
                || session.getStatus() == LivenessSessionStatus.EXPIRED) {
            sessionFrames.remove(session.getId());
            throw new BadRequestException("Sessao expirada. Crie uma nova sessao.");
        }
    }

    private void persistResult(LivenessSession session, LivenessResultDTO result) {
        try {
            session.setResultJson(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ignored) {}
        session.setStatus(LivenessSessionStatus.COMPLETED);
        sessionRepository.save(session);
    }

    private LivenessSessionResponseDTO toSessionResponse(LivenessSession session, int framesReceived) {
        return new LivenessSessionResponseDTO(
                session.getId().toString(),
                session.getChallengeType(),
                session.getChallengeType().getInstruction(),
                session.getChallengeType().getFramesRequired(),
                framesReceived,
                session.getStatus(),
                session.getExpiresAt());
    }

    // ── Record interno: carrega o resultado de um analisador individual ───
    private record AnalyzerResult(String name, double score, double weight,
                                  double threshold, String flag) {}
}
