package com.team.peektime_api.global.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.SendResult.AlreadyProcessed;
import com.team.peektime_api.global.outbox.SendResult.Success;
import com.team.peektime_api.global.outbox.SendResult.Unknown;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final OutboxTransactionManager transactionManager;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    // @Scheduled(fixedDelay = 60000)  // V1과 충돌 방지를 위해 주석 처리
    public void pollAndProcess() {
        List<OutboxEvent> events = transactionManager.fetchAndMarkProcessing();

        if (events.isEmpty()) {
            log.info("[V2] 기록해야 하는 미션이 존재하지 않습니다.");
            return;
        }

        log.info("[V2] Outbox 폴링: {}건 처리 시작", events.size());

        ProcessingResult result = processAllEvents(events);

        transactionManager.handleResults(result.toDelete(), result.toRetry());
    }

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

    private record ProcessingResult(List<Long> toDelete, List<Long> toRetry) {}
}