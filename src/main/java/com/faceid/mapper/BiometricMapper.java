package com.faceid.mapper;

import com.faceid.dto.FaceRecognitionDTO;
import com.faceid.model.BiometricData;

public class BiometricMapper {

    public static BiometricData toEntity(FaceRecognitionDTO dto) {
        BiometricData biometric = new BiometricData();
        biometric.setFaceTemplate(dto.getFaceImage());
        return biometric;
    }
}
