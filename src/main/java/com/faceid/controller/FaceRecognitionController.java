package com.faceid.controller;

import com.faceid.model.FaceVerificationRequest; // <-- Importe a nova classe
import com.faceid.model.User;
import com.faceid.service.FaceRecognitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/face")
public class FaceRecognitionController {

    private final FaceRecognitionService faceRecognitionService;

    public FaceRecognitionController(FaceRecognitionService faceRecognitionService) {
        this.faceRecognitionService = faceRecognitionService;
    }

    /**
     * Endpoint para registrar caracteristicas faciais de um usuario a partir de uma imagem via UPLOAD DE ARQUIVO.
     * Recebe a imagem via MultipartFile.
     * @param userId ID do usuario para associar a face.
     * @param file Imagem enviada como MultipartFile.
     * @return ResponseEntity com o usuario atualizado.
     */
    @PostMapping(value = "/register/file/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<User> registerFaceFromFile(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            byte[] imageData = file.getBytes();
            User updatedUser = faceRecognitionService.registerFace(userId, imageData);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint para registrar caracteristicas faciais de um usuario a partir de uma imagem via STRING BASE64.
     * Recebe um objeto JSON contendo a imagem como String Base64.
     * @param userId ID do usuario para associar a face.
     * @param request Objeto contendo a String Base64 da imagem.
     * @return ResponseEntity com o usuario atualizado.
     */
    @PostMapping(value = "/register/base64/{userId}", consumes = "application/json")
    public ResponseEntity<User> registerFaceFromBase64(
            @PathVariable Long userId,
            @RequestBody FaceVerificationRequest request) { // <-- CORRIGIDO
        try {
            String base64Image = request.getBase64Image(); // <-- CORRIGIDO
            if (base64Image == null || base64Image.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            // Remove prefixo "data:image/png;base64," se presente
            String cleanBase64 = base64Image.replaceAll("^data:image/[a-zA-Z]+;base64,", "");
            byte[] imageData = faceRecognitionService.decodeBase64ToBytes(cleanBase64);
            User updatedUser = faceRecognitionService.registerFace(userId, imageData);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint para verificar uma face em uma imagem contra as caracteristicas registradas de um usuario via UPLOAD DE ARQUIVO.
     * Recebe a imagem via MultipartFile.
     * @param userId ID do usuario para verificacao.
     * @param file Imagem enviada como MultipartFile.
     * @return ResponseEntity com o resultado da verificacao (true/false).
     */
    @PostMapping(value = "/verify/file/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<Boolean> verifyFaceFromFile(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            byte[] imageData = file.getBytes();
            boolean isMatch = faceRecognitionService.verifyFace(userId, imageData);
            return ResponseEntity.ok(isMatch);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint para verificar uma face em uma imagem contra as caracteristicas registradas de um usuario via STRING BASE64.
     * Recebe um objeto JSON contendo a imagem como String Base64.
     * @param userId ID do usuario para verificacao.
     * @param request Objeto contendo a String Base64 da imagem.
     * @return ResponseEntity com o resultado da verificacao (true/false).
     */
    @PostMapping(value = "/verify/base64/{userId}", consumes = "application/json")
    public ResponseEntity<Boolean> verifyFaceFromBase64(
            @PathVariable Long userId,
            @RequestBody FaceVerificationRequest request) { // <-- CORRIGIDO
        try {
            String base64Image = request.getBase64Image(); // <-- CORRIGIDO
            if (base64Image == null || base64Image.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            String cleanBase64 = base64Image.replaceAll("^data:image/[a-zA-Z]+;base64,", "");
            byte[] imageData = faceRecognitionService.decodeBase64ToBytes(cleanBase64);
            boolean isMatch = faceRecognitionService.verifyFace(userId, imageData);
            return ResponseEntity.ok(isMatch);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}