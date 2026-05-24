package com.team.peektime_api.global.outbox.repository;

import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT o FROM OutboxEvent o WHERE o.createdAt < :threshold ORDER BY o.createdAt",
            nativeQuery = false)
    List<OutboxEvent> findByCreatedAtBeforeForUpdate(@Param("threshold") LocalDateTime threshold);

    @Query(value = "SELECT * FROM outbox_event WHERE created_at < :threshold ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 5",
            nativeQuery = true)
    List<OutboxEvent> findByCreatedAtBeforeWithSkipLocked(@Param("threshold") LocalDateTime threshold);

    // V2: 상태 기반 조회 (READY만 + SKIP LOCKED)
    @Query(value = "SELECT * FROM outbox_event WHERE status = 'READY' AND created_at < :threshold ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 5",
            nativeQuery = true)
    List<OutboxEvent> findReadyEventsWithSkipLocked(@Param("threshold") LocalDateTime threshold);
}