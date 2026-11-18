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

    // V4/V5 claim: 재시도 시각이 도래한 READY만 — 백오프 대기 중인 건은 next_retry_at 조건으로 자연 제외.
    // 신규 이벤트도 next_retry_at(생성+3초)로 시작하므로 리스너 선공 유예가 이 조건 하나에 흡수됨.
    // 리스 만료 PROCESSING의 수거는 여기가 아니라 스위퍼(findExpiredProcessingWithSkipLocked)의 책임
    @Query(value = "SELECT * FROM outbox_event WHERE status = 'READY' AND next_retry_at <= :now ORDER BY next_retry_at LIMIT 30 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findClaimableWithSkipLocked(@Param("now") LocalDateTime now);

    // V5 스위퍼: 리스가 만료된 PROCESSING 조회 — 소유권 만료 판정이 updatedAt 추정(V2)이 아니라
    // claim이 직접 적어둔 마감(lease_until)이다. LIMIT은 비정상 대량 고아 상황에서 한 사이클의
    // 복구 폭(과 락 범위)을 제한. SKIP LOCKED: 다중 인스턴스 스위퍼가 겹쳐도 같은 행을 나눠 갖고,
    // 한쪽이 먼저 복구(READY 전이)한 행은 다른 쪽의 status 조건에서 빠져 이중 복구가 없다
    @Query(value = "SELECT * FROM outbox_event WHERE status = 'PROCESSING' AND lease_until < :now ORDER BY lease_until LIMIT 100 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findExpiredProcessingWithSkipLocked(@Param("now") LocalDateTime now);

    // stuck PROCESSING 조회 (updatedAt이 threshold 이전인 PROCESSING 상태)
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = com.team.peektime_api.global.outbox.entity.OutboxStatus.PROCESSING AND o.updatedAt < :threshold")
    List<OutboxEvent> findStuckProcessing(@Param("threshold") LocalDateTime threshold);

    // V3: 모든 이벤트 ID 조회
    @Query("SELECT o.id FROM OutboxEvent o ORDER BY o.createdAt")
    List<Long> findAllIds();
}