package com.faceid.controller;

import com.faceid.dto.ResponseDTO.FaceVerificationResultDTO;
import com.faceid.dto.ResponseDTO.UserResponseDTO;
import com.faceid.model.FaceVerificationRequest;
import com.faceid.model.User;
import com.faceid.security.FaceRateLimitService;
import com.faceid.service.FaceRecognitionService;
import com.faceid.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/face")
public class FaceRecognitionController {

    private final FaceRecognitionService faceRecognitionService;
    private final FaceRateLimitService   faceRateLimitService;

    public FaceRecognitionController(FaceRecognitionService faceRecognitionService,
                                     FaceRateLimitService faceRateLimitService) {
        this.faceRecognitionService = faceRecognitionService;
        this.faceRateLimitService   = faceRateLimitService;
    }

    @PostMapping(value = "/register/file/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<UserResponseDTO> registerFaceFromFile(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        faceRecognitionService.validateImageUpload(file);
        User updatedUser = faceRecognitionService.registerFace(userId, file.getBytes());
        return ResponseEntity.ok(toResponse(updatedUser));
    }

    @PostMapping(value = "/register/base64/{userId}", consumes = "application/json")
    public ResponseEntity<UserResponseDTO> registerFaceFromBase64(
            @PathVariable Long userId,
            @Valid @RequestBody FaceVerificationRequest request) throws IOException {
        String cleanBase64 = request.getBase64Image().replaceAll("^data:image/[a-zA-Z]+;base64,", "");
        byte[] imageData = faceRecognitionService.decodeBase64ToBytes(cleanBase64);
        User updatedUser = faceRecognitionService.registerFace(userId, imageData);
        return ResponseEntity.ok(toResponse(updatedUser));
    }

    /**
     * Verificação com rate limiting por IP — máx. {@link FaceRateLimitService#MAX_ATTEMPTS}
     * tentativas/hora para prevenir oracle attacks.
     */
    @PostMapping(value = "/verify/file/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<FaceVerificationResultDTO> verifyFaceFromFile(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) throws IOException {
        checkFaceRateLimit(httpRequest);
        faceRecognitionService.validateImageUpload(file);
        return ResponseEntity.ok(faceRecognitionService.verifyFace(userId, file.getBytes()));
    }

    @PostMapping(value = "/verify/base64/{userId}", consumes = "application/json")
    public ResponseEntity<FaceVerificationResultDTO> verifyFaceFromBase64(
            @PathVariable Long userId,
            @Valid @RequestBody FaceVerificationRequest request,
            HttpServletRequest httpRequest) {
        checkFaceRateLimit(httpRequest);
        String clean = request.getBase64Image().replaceAll("^data:image/[a-zA-Z]+;base64,", "");
        byte[] imageData = faceRecognitionService.decodeBase64ToBytes(clean);
        return ResponseEntity.ok(faceRecognitionService.verifyFace(userId, imageData));
    }

    private void checkFaceRateLimit(HttpServletRequest request) {
        String ip = IpUtils.extractClientIp(request);
        if (faceRateLimitService.isBlocked(ip)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Limite de verificacoes excedido. Tente novamente em 1 hora.");
        }
        faceRateLimitService.recordAttempt(ip);
    }

    private UserResponseDTO toResponse(User user) {
        boolean faceRegistered = user.getFaceFeatures() != null && user.getFaceFeatures().length > 0;
        return new UserResponseDTO(
                user.getId(), user.getUsername(), user.getFullName(),
                user.getCpf(), user.getEmail(), user.getPhone(),
                user.getBirthDate(), faceRegistered);
    }
}
