package com.faceid.repository;

import com.faceid.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    /** Remove tokens cujo JWT original já teria expirado — chamado pelo scheduler. */
    void deleteByExpiresAtBefore(Instant cutoff);
}
