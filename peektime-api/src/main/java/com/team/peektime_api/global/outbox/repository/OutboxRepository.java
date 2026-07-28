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

    @Query(value = "SELECT * FROM outbox_event WHERE created_at < :threshold ORDER BY created_at LIMIT 5 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findByCreatedAtBeforeWithSkipLocked(@Param("threshold") LocalDateTime threshold);

    // V2: 상태 기반 조회 (READY만 + SKIP LOCKED)
    @Query(value = "SELECT * FROM outbox_event WHERE status = 'READY' AND created_at < :threshold ORDER BY created_at LIMIT 5 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findReadyEventsWithSkipLocked(@Param("threshold") LocalDateTime threshold);

    // V4: 재시도 시각이 도래한 READY만 claim — 백오프 대기 중인 건은 next_retry_at 조건으로 자연 제외.
    // 신규 이벤트도 next_retry_at(생성+3초)로 시작하므로 리스너 선공 유예가 이 조건 하나에 흡수됨
    @Query(value = "SELECT * FROM outbox_event WHERE status = 'READY' AND next_retry_at <= :now ORDER BY next_retry_at LIMIT 30 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findClaimableWithSkipLocked(@Param("now") LocalDateTime now);

    // stuck PROCESSING 조회 (updatedAt이 threshold 이전인 PROCESSING 상태)
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = com.team.peektime_api.global.outbox.entity.OutboxStatus.PROCESSING AND o.updatedAt < :threshold")
    List<OutboxEvent> findStuckProcessing(@Param("threshold") LocalDateTime threshold);

    // V3: 모든 이벤트 ID 조회
    @Query("SELECT o.id FROM OutboxEvent o ORDER BY o.createdAt")
    List<Long> findAllIds();
}