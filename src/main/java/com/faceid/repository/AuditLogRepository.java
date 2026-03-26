package com.faceid.repository;

import com.faceid.model.AuditEvent;
import com.faceid.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);

    List<AuditLog> findByIpAddressAndEventOrderByTimestampDesc(String ipAddress, AuditEvent event);

    long countByIpAddressAndEventAndTimestampAfter(String ipAddress, AuditEvent event, Instant since);

    void deleteByTimestampBefore(Instant cutoff);
}
