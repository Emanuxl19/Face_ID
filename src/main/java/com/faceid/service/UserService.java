package com.faceid.service;

import com.faceid.dto.RequestDTO.UserRequestDTO;
import com.faceid.dto.ResponseDTO.UserResponseDTO;
import com.faceid.exception.ConflictException;
import com.faceid.exception.ResourceNotFoundException;
import com.faceid.model.User;
import com.faceid.repository.UserRepository;
import com.faceid.util.SecurityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── UserDetailsService (Spring Security) ─────────────────────────────

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())   // "USER" → ROLE_USER, "ADMIN" → ROLE_ADMIN
                .build();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = findOrThrow(id);
        SecurityUtils.requireSelf(user);
        return toResponse(user);
    }

    public UserResponseDTO findByCpf(String cpf) {
        return userRepository.findByCpf(normalizeCpf(cpf))
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com CPF informado."));
    }

    public UserResponseDTO findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com e-mail informado."));
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        String username = dto.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Ja existe um usuario com esse nome.");
        }
        if (dto.getCpf() != null && userRepository.existsByCpf(normalizeCpf(dto.getCpf()))) {
            throw new ConflictException("Ja existe um usuario com esse CPF.");
        }
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new ConflictException("Ja existe um usuario com esse e-mail.");
        }

        User user = new User(username, passwordEncoder.encode(dto.getPassword()));
        applyPersonalData(user, dto);
        return toResponse(userRepository.save(user));
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = findOrThrow(id);
        SecurityUtils.requireSelf(user);
        String username = dto.getUsername().trim();

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new ConflictException("Ja existe um usuario com esse nome.");
        }
        String normalizedCpf = dto.getCpf() != null ? normalizeCpf(dto.getCpf()) : null;
        if (normalizedCpf != null && !normalizedCpf.equals(user.getCpf())
                && userRepository.existsByCpf(normalizedCpf)) {
            throw new ConflictException("Ja existe um usuario com esse CPF.");
        }
        String normalizedEmail = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null;
        if (normalizedEmail != null && !normalizedEmail.equals(user.getEmail())
                && userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Ja existe um usuario com esse e-mail.");
        }

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        applyPersonalData(user, dto);
        return toResponse(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = findOrThrow(id);
        SecurityUtils.requireSelf(user);
        userRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com ID: " + id));
    }

    private void applyPersonalData(User user, UserRequestDTO dto) {
        user.setFullName(dto.getFullName());
        user.setCpf(dto.getCpf() != null ? normalizeCpf(dto.getCpf()) : null);
        user.setEmail(dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null);
        user.setPhone(dto.getPhone());
        user.setBirthDate(dto.getBirthDate());
    }

    /** Remove pontos e traço do CPF, mantém somente dígitos. */
    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("[.\\-]", "");
    }

    private UserResponseDTO toResponse(User user) {
        boolean faceRegistered = user.getFaceFeatures() != null && user.getFaceFeatures().length > 0;
        return new UserResponseDTO(
                user.getId(), user.getUsername(), user.getFullName(),
                maskCpf(user.getCpf()), user.getEmail(), user.getPhone(),
                user.getBirthDate(), faceRegistered);
    }

    /**
     * Mascara o CPF para exibição: 11 dígitos → "***.***.***-XX".
     * Os dois dígitos verificadores são preservados; o restante é ocultado.
     */
    private static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return cpf;
        return "***.***.***-" + cpf.substring(9);
    }
}
