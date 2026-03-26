package com.faceid.repository;

import com.faceid.model.FaceRegistrationSession;
import com.faceid.model.FaceRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FaceRegistrationSessionRepository extends JpaRepository<FaceRegistrationSession, String> {

    List<FaceRegistrationSession> findByUserIdAndStatus(Long userId, FaceRegistrationStatus status);

    List<FaceRegistrationSession> findByExpiresAtBeforeAndStatus(Instant before, FaceRegistrationStatus status);
}
