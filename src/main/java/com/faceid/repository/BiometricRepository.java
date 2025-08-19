package com.faceid.repository;

import com.faceid.model.BiometricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiometricRepository extends JpaRepository<BiometricData, Long> {
    // Aqui você pode adicionar consultas personalizadas se necessário
    // Exemplo: encontrar biometric data por usuário
    BiometricData findByUserId(Long userId);
}