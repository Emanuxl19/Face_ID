package com.faceid.repository;

import com.faceid.liveness.LivenessSessionStatus;
import com.faceid.model.LivenessSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LivenessSessionRepository extends JpaRepository<LivenessSession, UUID> {

    /** Retorna sessões expiradas ainda não marcadas como EXPIRED (para limpeza agendada). */
    List<LivenessSession> findByExpiresAtBeforeAndStatusNot(Instant now, LivenessSessionStatus status);
}
