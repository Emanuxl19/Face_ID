package com.faceid.service;

import com.faceid.dto.ResponseDTO.AngleSubmitResultDTO;
import com.faceid.dto.ResponseDTO.RegistrationSessionResponseDTO;
import com.faceid.exception.BadRequestException;
import com.faceid.exception.ResourceNotFoundException;
import com.faceid.model.FaceAngleImage;
import com.faceid.model.FaceDetectionData;
import com.faceid.model.FacePose;
import com.faceid.model.FaceRegistrationSession;
import com.faceid.model.FaceRegistrationStatus;
import com.faceid.model.User;
import com.faceid.repository.FaceAngleImageRepository;
import com.faceid.repository.FaceRegistrationSessionRepository;
import com.faceid.repository.UserRepository;
import com.faceid.util.PoseEstimator;
import com.faceid.util.SecurityUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gerencia o fluxo de cadastro facial multi-ângulo (estilo Apple Face ID).
 *
 * <p><b>Fluxo completo:</b>
 * <ol>
 *   <li>{@code startSession(userId)} — cria uma sessão com TTL de 5 minutos e define as poses obrigatórias.</li>
 *   <li>{@code submitFrame(sessionId, userId, imageData)} — processa um frame:
 *       detecta o rosto, estima a pose, armazena os features se for novidade, e avança a sessão.</li>
 *   <li>A sessão avança automaticamente para {@link FaceRegistrationStatus#COMPLETE} quando todas as poses
 *       obrigatórias ({@link #REQUIRED_POSES}) forem capturadas. Ao concluir, a pose FRONT é salva
 *       em {@code user.faceFeatures} para manter compatibilidade com o campo {@code faceRegistered}.</li>
 *   <li>{@code getSession(sessionId)} — retorna o estado atual da sessão a qualquer momento.</li>
 * </ol>
 *
 * <p><b>Poses obrigatórias:</b> FRONT, LEFT, RIGHT — mínimo para identificação robusta.
 * UP e DOWN são bonus opcionais (capturadas se o usuário as oferecer).
 *
 * <p><b>Verificação:</b> {@link FaceRecognitionService#verifyFace} usa todas as poses registradas
 * em {@link com.faceid.repository.FaceAngleImageRepository} e aceita se qualquer ângulo
 * superar o limiar de similaridade (fallback para {@code user.faceFeatures} se sem multi-ângulo).
 */
@Service
public class MultiAngleRegistrationService {

    /** Poses obrigatórias para concluir o cadastro. */
    public static final Set<FacePose> REQUIRED_POSES = Set.of(
            FacePose.FRONT, FacePose.LEFT, FacePose.RIGHT
    );

    private static final Duration SESSION_TTL = Duration.ofMinutes(5);

    private final FaceAngleImageRepository faceAngleImageRepository;
    private final FaceRegistrationSessionRepository sessionRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final UserRepository userRepository;

    public MultiAngleRegistrationService(
            FaceAngleImageRepository faceAngleImageRepository,
            FaceRegistrationSessionRepository sessionRepository,
            FaceRecognitionService faceRecognitionService,
            UserRepository userRepository) {
        this.faceAngleImageRepository = faceAngleImageRepository;
        this.sessionRepository        = sessionRepository;
        this.faceRecognitionService   = faceRecognitionService;
        this.userRepository           = userRepository;
    }

    // ── Iniciar sessão ──────────────────────────────────────────────────────

    /**
     * Cria uma nova sessão de cadastro para o usuário.
     * Quaisquer sessões PENDING anteriores são expiradas.
     * Ângulos já cadastrados são removidos (re-cadastro limpa o histórico).
     */
    @Transactional(rollbackFor = Exception.class)
    public RegistrationSessionResponseDTO startSession(Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com ID: " + userId));
        SecurityUtils.requireSelf(owner);

        // Expira sessões anteriores deste usuário
        sessionRepository.findByUserIdAndStatus(userId, FaceRegistrationStatus.PENDING)
                .forEach(s -> {
                    s.setStatus(FaceRegistrationStatus.EXPIRED);
                    sessionRepository.save(s);
                });

        // Re-cadastro: remove ângulos antigos e limpa faceFeatures do usuário
        faceAngleImageRepository.deleteByUserId(userId);
        userRepository.findById(userId).ifPresent(user -> {
            user.setFaceFeatures(null);
            userRepository.save(user);
        });

        FaceRegistrationSession session = new FaceRegistrationSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus(FaceRegistrationStatus.PENDING);
        session.setCollectedPoses("");
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(SESSION_TTL));
        sessionRepository.save(session);

        return toSessionResponse(session);
    }

    // ── Submeter frame ──────────────────────────────────────────────────────

    /**
     * Processa um frame enviado pelo cliente:
     * <ol>
     *   <li>Valida a sessão (existência, propriedade, expiração, status).</li>
     *   <li>Detecta o rosto e estima a pose com {@link PoseEstimator}.</li>
     *   <li>Se a pose for nova, persiste {@link FaceAngleImage} e atualiza a sessão.</li>
     *   <li>Se todas as poses obrigatórias forem coletadas, conclui a sessão e
     *       atualiza {@code user.faceFeatures} com os features FRONT.</li>
     * </ol>
     *
     * @param sessionId ID da sessão retornado por {@code startSession}.
     * @param userId    ID do usuário dono da sessão (validado contra a sessão).
     * @param imageData Bytes brutos da imagem (JPEG ou PNG, máx. 1 MB).
     */
    @Transactional(rollbackFor = Exception.class)
    public AngleSubmitResultDTO submitFrame(String sessionId, Long userId, byte[] imageData) {
        FaceRegistrationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessao nao encontrada: " + sessionId));

        validateSession(session, userId);

        // Detecta face e estima pose
        FaceDetectionData detection = faceRecognitionService.detectFaceWithData(imageData);
        FacePose detectedPose = PoseEstimator.estimatePose(detection.faceRect(), detection.fullGrayImage());

        Set<FacePose> collected = session.getCollectedPosesAsSet();
        boolean alreadyCollected = collected.contains(detectedPose);

        if (!alreadyCollected) {
            byte[] features = faceRecognitionService.extractFaceFeatures(detection.croppedFace());
            faceAngleImageRepository.save(new FaceAngleImage(userId, detectedPose, features));
            collected.add(detectedPose);
            session.setCollectedPosesFromSet(collected);
        }

        // Verifica se todas as poses obrigatórias foram capturadas
        boolean complete = collected.containsAll(REQUIRED_POSES);
        if (complete && session.getStatus() == FaceRegistrationStatus.PENDING) {
            session.setStatus(FaceRegistrationStatus.COMPLETE);
            promoteFrontFeaturesAsLegacyFace(userId);
        }

        sessionRepository.save(session);

        Set<FacePose> remaining = new HashSet<>(REQUIRED_POSES);
        remaining.removeAll(collected);

        String instruction = remaining.stream()
                .findFirst()
                .map(FacePose::getInstruction)
                .orElse(complete ? "Cadastro concluido com sucesso!" : null);

        return new AngleSubmitResultDTO(
                sessionId, detectedPose, alreadyCollected,
                Set.copyOf(collected), Set.copyOf(remaining),
                session.getStatus(), session.getExpiresAt(), instruction);
    }

    // ── Consultar sessão ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RegistrationSessionResponseDTO getSession(String sessionId) {
        FaceRegistrationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessao nao encontrada: " + sessionId));
        return toSessionResponse(session);
    }

    // ── Limpeza agendada ────────────────────────────────────────────────────

    /**
     * Marca como EXPIRED sessões PENDING cujo TTL passou.
     * Executado a cada 60 segundos.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.findByExpiresAtBeforeAndStatus(Instant.now(), FaceRegistrationStatus.PENDING)
                .forEach(s -> {
                    s.setStatus(FaceRegistrationStatus.EXPIRED);
                    sessionRepository.save(s);
                });
    }

    // ── Helpers privados ────────────────────────────────────────────────────

    private void validateSession(FaceRegistrationSession session, Long userId) {
        if (!session.getUserId().equals(userId)) {
            throw new BadRequestException("Sessao nao pertence ao usuario informado.");
        }
        if (Instant.now().isAfter(session.getExpiresAt()) ||
                session.getStatus() == FaceRegistrationStatus.EXPIRED) {
            session.setStatus(FaceRegistrationStatus.EXPIRED);
            sessionRepository.save(session);
            throw new BadRequestException("Sessao expirada. Inicie uma nova sessao de cadastro.");
        }
        if (session.getStatus() == FaceRegistrationStatus.COMPLETE) {
            throw new BadRequestException("Sessao ja concluida. O cadastro multi-angulo esta completo.");
        }
    }

    /**
     * Quando o cadastro multi-ângulo é concluído, persiste os features da pose FRONT
     * em {@code user.faceFeatures} para manter compatibilidade com o campo
     * {@code faceRegistered} do {@code UserResponseDTO}.
     */
    private void promoteFrontFeaturesAsLegacyFace(Long userId) {
        faceAngleImageRepository.findByUserId(userId).stream()
                .filter(img -> img.getPose() == FacePose.FRONT)
                .findFirst()
                .ifPresent(frontImg ->
                        userRepository.findById(userId).ifPresent(user -> {
                            user.setFaceFeatures(frontImg.getFaceFeatures());
                            userRepository.save(user);
                        })
                );
    }

    private RegistrationSessionResponseDTO toSessionResponse(FaceRegistrationSession session) {
        Set<FacePose> collected  = session.getCollectedPosesAsSet();
        Set<FacePose> remaining  = new HashSet<>(REQUIRED_POSES);
        remaining.removeAll(collected);

        return new RegistrationSessionResponseDTO(
                session.getId(), session.getUserId(), session.getStatus(),
                REQUIRED_POSES, Set.copyOf(collected), Set.copyOf(remaining),
                session.getExpiresAt());
    }
}
