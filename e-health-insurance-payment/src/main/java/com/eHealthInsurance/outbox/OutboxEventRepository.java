package com.eHealthInsurance.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE status IN ('PENDING', 'FAILED', 'DLQ_PENDING')
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findNextBatchForUpdate(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
