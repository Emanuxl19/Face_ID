package com.faceid.controller;

import com.faceid.dto.AuthRequestDTO;
import com.faceid.dto.AuthResponseDTO;
import com.faceid.model.User;
import com.faceid.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Endpoint de login
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    // Endpoint de registro de usuário (opcional)
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User newUser = authService.registerUser(user);
        return ResponseEntity.ok(newUser);
    }
}
