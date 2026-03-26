package com.faceid.repository;

import com.faceid.model.FaceAngleImage;
import com.faceid.model.FacePose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceAngleImageRepository extends JpaRepository<FaceAngleImage, Long> {

    List<FaceAngleImage> findByUserId(Long userId);

    boolean existsByUserIdAndPose(Long userId, FacePose pose);

    /** Remove todos os ângulos de um usuário (ex: re-cadastro). */
    void deleteByUserId(Long userId);
}
