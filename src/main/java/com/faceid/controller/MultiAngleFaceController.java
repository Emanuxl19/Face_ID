package com.faceid.controller;

import com.faceid.dto.ResponseDTO.AngleSubmitResultDTO;
import com.faceid.dto.ResponseDTO.RegistrationSessionResponseDTO;
import com.faceid.model.FacePose;
import com.faceid.model.FaceRegistrationSession;
import com.faceid.model.FaceVerificationRequest;
import com.faceid.service.FaceRecognitionService;
import com.faceid.service.MultiAngleRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * Endpoints para cadastro facial multi-ângulo (estilo Apple Face ID).
 *
 * <pre>
 * POST /api/face/registration/start/{userId}          → inicia sessão, retorna poses obrigatórias
 * POST /api/face/registration/{sessionId}/frame/file  → envia frame como arquivo
 * POST /api/face/registration/{sessionId}/frame/base64→ envia frame como Base64
 * GET  /api/face/registration/{sessionId}             → consulta estado da sessão
 * GET  /api/face/registration/poses                   → lista as poses disponíveis com instruções
 * </pre>
 *
 * <b>Fluxo típico do cliente:</b>
 * <ol>
 *   <li>Chama {@code start} para obter o {@code sessionId} e as poses necessárias.</li>
 *   <li>Exibe instrução ao usuário (ex: "Vire a cabeça levemente para a esquerda").</li>
 *   <li>Captura frame e envia para {@code /frame/file} ou {@code /frame/base64}.</li>
 *   <li>Repete enquanto {@code remainingPoses} não estiver vazio.</li>
 *   <li>Quando {@code status == COMPLETE}, o cadastro está concluído — verificação já funciona.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/face/registration")
public class MultiAngleFaceController {

    private final MultiAngleRegistrationService registrationService;
    private final FaceRecognitionService faceRecognitionService;

    public MultiAngleFaceController(MultiAngleRegistrationService registrationService,
                                    FaceRecognitionService faceRecognitionService) {
        this.registrationService  = registrationService;
        this.faceRecognitionService = faceRecognitionService;
    }

    /**
     * Inicia uma nova sessão de cadastro multi-ângulo para o usuário.
     * Expira sessões PENDING anteriores e apaga ângulos já registrados (re-cadastro).
     */
    @PostMapping("/start/{userId}")
    public ResponseEntity<RegistrationSessionResponseDTO> startSession(@PathVariable Long userId) {
        return ResponseEntity.ok(registrationService.startSession(userId));
    }

    /**
     * Submete um frame via upload de arquivo.
     * O servidor detecta o rosto, estima a pose e registra o ângulo se ainda não capturado.
     */
    @PostMapping(value = "/{sessionId}/frame/file", consumes = "multipart/form-data")
    public ResponseEntity<AngleSubmitResultDTO> submitFrameFromFile(
            @PathVariable String sessionId,
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        faceRecognitionService.validateImageUpload(file);
        AngleSubmitResultDTO result = registrationService.submitFrame(sessionId, userId, file.getBytes());
        return ResponseEntity.ok(result);
    }

    /**
     * Submete um frame via JSON com imagem em Base64.
     */
    @PostMapping(value = "/{sessionId}/frame/base64", consumes = "application/json")
    public ResponseEntity<AngleSubmitResultDTO> submitFrameFromBase64(
            @PathVariable String sessionId,
            @RequestParam Long userId,
            @Valid @RequestBody FaceVerificationRequest request) {
        String clean = request.getBase64Image().replaceAll("^data:image/[a-zA-Z]+;base64,", "");
        byte[] imageData = faceRecognitionService.decodeBase64ToBytes(clean);
        AngleSubmitResultDTO result = registrationService.submitFrame(sessionId, userId, imageData);
        return ResponseEntity.ok(result);
    }

    /**
     * Retorna o estado atual de uma sessão (poses coletadas, faltantes, status e expiração).
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<RegistrationSessionResponseDTO> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(registrationService.getSession(sessionId));
    }

    /**
     * Lista todas as poses disponíveis com suas instruções para o usuário.
     * Útil para o frontend exibir as orientações corretas antes de iniciar a sessão.
     */
    @GetMapping("/poses")
    public ResponseEntity<PoseCatalogResponse> listPoses() {
        PoseInfo[] infos = java.util.Arrays.stream(FacePose.values())
                .map(p -> new PoseInfo(p.name(), p.getInstruction(),
                        MultiAngleRegistrationService.REQUIRED_POSES.contains(p)))
                .toArray(PoseInfo[]::new);
        return ResponseEntity.ok(new PoseCatalogResponse(
                Set.copyOf(MultiAngleRegistrationService.REQUIRED_POSES), infos));
    }

    public record PoseInfo(String pose, String instruction, boolean required) {}
    public record PoseCatalogResponse(Set<FacePose> requiredPoses, PoseInfo[] allPoses) {}
}
