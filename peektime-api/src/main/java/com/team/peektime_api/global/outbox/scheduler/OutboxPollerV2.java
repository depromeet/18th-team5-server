package com.team.peektime_api.global.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.SendResult.AlreadyProcessed;
import com.team.peektime_api.global.outbox.SendResult.Success;
import com.team.peektime_api.global.outbox.SendResult.Unknown;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OutboxPoller V2 - 트랜잭션 분리 버전
 *
 * V1과의 차이점:
 * - V1: 전체가 하나의 트랜잭션 (조회 + 외부 API 호출 + 삭제)
 * - V2: 트랜잭션 분리 (Tx1: 조회+상태변경, 외부 API 호출, Tx2: 삭제)
 *
 * 장점:
 * - DB 커넥션 점유 시간 최소화
 * - 외부 API 호출 중 락을 잡지 않음
 * - 상태 기반으로 중복 처리 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollerV2 {

    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    // @Scheduled(fixedDelay = 60000)  // V1과 충돌 방지를 위해 주석 처리
    public void pollAndProcess() {
        // Tx1: 조회 + 상태 변경 (READY → PROCESSING)
        List<OutboxEvent> events = fetchAndMarkProcessing();

        if (events.isEmpty()) {
            log.info("[V2] 기록해야 하는 미션이 존재하지 않습니다.");
            return;
        }

        log.info("[V2] Outbox 폴링: {}건 처리 시작", events.size());

        // 외부 API 호출 (트랜잭션 밖)
        ProcessingResult result = processAllEvents(events);

        // Tx2: 성공/중복은 삭제, 실패는 READY로 복구
        handleResults(result);
    }

    /**
     * Tx1: READY 상태인 이벤트 조회 후 PROCESSING으로 변경
     */
    private List<OutboxEvent> fetchAndMarkProcessing() {
        return transactionTemplate.execute(status -> {
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
            List<OutboxEvent> events = outboxRepository.findReadyEventsWithSkipLocked(threshold);

            for (OutboxEvent event : events) {
                event.markProcessing();
            }

            return events;
        });
    }

    /**
     * 외부 API 호출 (트랜잭션 밖)
     */
    private ProcessingResult processAllEvents(List<OutboxEvent> events) {
        List<Long> toDelete = new ArrayList<>();
        List<Long> toRetry = new ArrayList<>();

        for (OutboxEvent event : events) {
            SendResult result = processEvent(event);

            if (result instanceof Success s) {
                toDelete.add(s.eventId());
            } else if (result instanceof AlreadyProcessed a) {
                toDelete.add(a.eventId());
            } else if (result instanceof Unknown u) {
                toRetry.add(u.eventId());
                log.warn("[V2] Outbox 결과 알 수 없음: id={}, reason={}", u.eventId(), u.reason());
            }
        }

        return new ProcessingResult(toDelete, toRetry);
    }

    private SendResult processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            return adminClient.sendMissionLogWithResult(payload, event.getId());

        } catch (Exception e) {
            return new Unknown(event.getId(), e.getMessage());
        }
    }

    /**
     * Tx2: 결과에 따라 삭제 또는 상태 복구
     */
    private void handleResults(ProcessingResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            // 성공/중복은 삭제
            if (!result.toDelete().isEmpty()) {
                outboxRepository.deleteAllByIdInBatch(result.toDelete());
                log.info("[V2] Outbox 삭제 완료: {}건", result.toDelete().size());
            }

            // 실패는 READY로 복구 (다음 폴링에서 재시도)
            if (!result.toRetry().isEmpty()) {
                List<OutboxEvent> retryEvents = outboxRepository.findAllById(result.toRetry());
                for (OutboxEvent event : retryEvents) {
                    event.markReady();
                }
                log.warn("[V2] Outbox 재시도 대기: {}건", result.toRetry().size());
            }
        });
    }

    private record ProcessingResult(List<Long> toDelete, List<Long> toRetry) {}
}