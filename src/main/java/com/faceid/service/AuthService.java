package com.faceid.service;

import com.faceid.dto.AuthRequestDTO;
import com.faceid.dto.AuthResponseDTO;
import com.faceid.model.User;
import com.faceid.repository.UserRepository;
import com.faceid.util.JwtUtil;
import com.faceid.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Autenticação via username e password
    public AuthResponseDTO authenticate(AuthRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha inválida");
        }

        // Gera token JWT
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponseDTO(token);
    }

    // Criação de usuário novo (opcional)
    public User registerUser(User user) {
        // Criptografa a senha antes de salvar
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}
