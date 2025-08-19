package com.faceid.controller;

import com.faceid.dto.FaceRecognitionDTO;
import com.faceid.model.BiometricData;
import com.faceid.service.FaceRecognitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/face")
public class FaceRecognitionController {

    private final FaceRecognitionService faceService;

    public FaceRecognitionController(FaceRecognitionService faceService) {
        this.faceService = faceService;
    }

    @PostMapping("/register")
    public ResponseEntity<BiometricData> registerFace(@RequestBody FaceRecognitionDTO dto) {
        BiometricData biometric = faceService.registerFace(dto);
        return ResponseEntity.ok(biometric);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticate(@RequestBody FaceRecognitionDTO dto) {
        boolean success = faceService.authenticate(dto);
        return ResponseEntity.ok(success ? "Autenticação bem-sucedida" : "Falha na autenticação");
    }
}
