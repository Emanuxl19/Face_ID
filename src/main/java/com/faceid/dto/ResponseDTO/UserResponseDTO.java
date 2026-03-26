package com.faceid.dto.ResponseDTO;

import java.time.LocalDate;

public record UserResponseDTO(
        Long id,
        String username,
        String fullName,
        String cpf,
        String email,
        String phone,
        LocalDate birthDate,
        boolean faceRegistered
) {}
