package com.faceid.service;

import com.faceid.dto.FaceRecognitionDTO;
import com.faceid.exception.UserNotFoundException;
import com.faceid.mapper.BiometricMapper;
import com.faceid.model.BiometricData;
import com.faceid.model.User;
import com.faceid.repository.BiometricRepository;
import com.faceid.repository.UserRepository;
import com.faceid.util.FaceRecognitionHelper;
import org.springframework.stereotype.Service;

@Service
public class FaceRecognitionService {

    private final UserRepository userRepository;
    private final BiometricRepository biometricRepository;

    public FaceRecognitionService(UserRepository userRepository, BiometricRepository biometricRepository) {
        this.userRepository = userRepository;
        this.biometricRepository = biometricRepository;
    }

    // Registra a face do usuário
    public BiometricData registerFace(FaceRecognitionDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        BiometricData biometric = BiometricMapper.toEntity(dto);
        biometric.setUser(user);
        user.setBiometricData(biometric);

        biometricRepository.save(biometric);
        userRepository.save(user);

        return biometric;
    }

    // Autentica a face do usuário
    public boolean authenticate(FaceRecognitionDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        BiometricData biometricData = user.getBiometricData();
        if (biometricData == null) return false;

        return FaceRecognitionHelper.compareFaces(biometricData.getFaceTemplate(), dto.getFaceImage());
    }
}
