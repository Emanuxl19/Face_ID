package com.faceid.controller;

import com.faceid.dto.ResponseDTO.LivenessResultDTO;
import com.faceid.dto.ResponseDTO.LivenessSessionResponseDTO;
import com.faceid.liveness.ChallengeType;
import com.faceid.model.FaceVerificationRequest;
import com.faceid.service.FaceRecognitionService;
import com.faceid.service.LivenessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * API de detecção de vivacidade (anti-spoofing).
 *
 * ── Análise passiva (frame único): ──────────────────────────────────────────
 *   POST /api/liveness/analyze          → upload de arquivo
 *   POST /api/liveness/analyze/base64   → imagem em Base64
 *
 * ── Desafio ativo (sessão multi-frame): ─────────────────────────────────────
 *   POST /api/liveness/session                       → cria sessão + retorna desafio
 *   POST /api/liveness/session/{id}/frame            → envia frame da câmera
 *   POST /api/liveness/session/{id}/evaluate         → avalia e retorna resultado
 *   GET  /api/liveness/session/{id}                  → status atual da sessão
 */
@RestController
@RequestMapping("/api/liveness")
public class LivenessController {

    private final LivenessService livenessService;
    private final FaceRecognitionService faceRecognitionService;

    public LivenessController(LivenessService livenessService,
                              FaceRecognitionService faceRecognitionService) {
        this.livenessService = livenessService;
        this.faceRecognitionService = faceRecognitionService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANÁLISE PASSIVA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Análise passiva de vivacidade a partir de upload de arquivo (JPG/PNG, máx 1 MB).
     * Aplica: variância Laplaciana + entropia LBP + gradiente Sobel.
     */
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<LivenessResultDTO> analyzeFromFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        faceRecognitionService.validateImageUpload(file);
        LivenessResultDTO result = livenessService.analyzePassive(file.getBytes());
        return ResponseEntity.ok(result);
    }

    /**
     * Análise passiva de vivacidade a partir de imagem em Base64.
     * Aceita prefixo data URI (ex: "data:image/png;base64,...").
     */
    @PostMapping(value = "/analyze/base64", consumes = "application/json")
    public ResponseEntity<LivenessResultDTO> analyzeFromBase64(
            @Valid @RequestBody FaceVerificationRequest request) {

        String clean = request.getBase64Image()
                .replaceAll("^data:image/[a-zA-Z]+;base64,", "");
        byte[] imageData = faceRecognitionService.decodeBase64ToBytes(clean);
        LivenessResultDTO result = livenessService.analyzePassive(imageData);
        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DESAFIO ATIVO — SESSÃO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cria uma nova sessão de desafio de vivacidade.
     *
     * @param userId        ID do usuário (opcional; null para sessão anônima).
     * @param challengeType PASSIVE | BLINK | HEAD_NOD | HEAD_SHAKE
     */
    @PostMapping("/session")
    public ResponseEntity<LivenessSessionResponseDTO> createSession(
            @RequestParam(required = false) Long userId,
            @RequestParam ChallengeType challengeType) {

        LivenessSessionResponseDTO session = livenessService.createSession(userId, challengeType);
        return ResponseEntity.status(201).body(session);
    }

    /**
     * Adiciona um frame capturado da câmera à sessão em andamento.
     * Deve ser chamado repetidamente enquanto o usuário executa o desafio.
     */
    @PostMapping(value = "/session/{sessionId}/frame", consumes = "multipart/form-data")
    public ResponseEntity<LivenessSessionResponseDTO> addFrame(
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file) throws IOException {

        faceRecognitionService.validateImageUpload(file);
        LivenessSessionResponseDTO status = livenessService.addFrame(sessionId, file.getBytes());
        return ResponseEntity.ok(status);
    }

    /**
     * Avalia todos os frames da sessão e retorna o resultado de vivacidade.
     * Chamado após o usuário concluir o desafio (ou quando framesReceived >= framesRequired).
     */
    @PostMapping("/session/{sessionId}/evaluate")
    public ResponseEntity<LivenessResultDTO> evaluate(@PathVariable UUID sessionId) {
        LivenessResultDTO result = livenessService.evaluateSession(sessionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Consulta o estado atual de uma sessão sem avaliá-la.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<LivenessSessionResponseDTO> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(livenessService.getSession(sessionId));
    }
}
