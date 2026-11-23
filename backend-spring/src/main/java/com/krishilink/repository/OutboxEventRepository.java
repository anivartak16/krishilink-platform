package com.krishilink.repository;

import com.krishilink.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e.id FROM OutboxEvent e WHERE e.publishedAt IS NULL AND e.attempts < :maxAttempts ORDER BY e.id")
    List<Long> findPendingIds(@Param("maxAttempts") int maxAttempts, Pageable page);

    /** Claims one pending event; another relay instance holding the row lock makes this return empty. */
    @Query(value = "SELECT * FROM outbox_events WHERE id = :id AND published_at IS NULL FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<OutboxEvent> claimPending(@Param("id") Long id);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.attempts = e.attempts + 1, e.lastError = :error WHERE e.id = :id")
    void recordFailure(@Param("id") Long id, @Param("error") String error);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);
}
