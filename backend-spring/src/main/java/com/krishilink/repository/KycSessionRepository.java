package com.krishilink.repository;

import com.krishilink.entity.KycSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycSessionRepository extends JpaRepository<KycSession, Long> {
    Optional<KycSession> findByClientId(String clientId);
}
