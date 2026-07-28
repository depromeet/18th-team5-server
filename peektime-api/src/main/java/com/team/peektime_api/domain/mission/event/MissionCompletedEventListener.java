package com.team.peektime_api.domain.mission.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import com.team.peektime_api.global.outbox.scheduler.v4.OutboxV4TransactionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 미션 완료 이벤트를 받아 Admin 서버로 로그를 전송하는 리스너 — 폴러를 기다리지 않는 "0번째 시도"
 *
 * - outbox row에 저장된 payload를 그대로 전송 (payload 생성 지점은 서비스 한 곳)
 * - 폴러와 동일한 payload를 보내므로 멱등키 불일치가 구조적으로 불가능
 * - 실패 시 아무것도 하지 않는다: row가 READY로 남아 폴러가 수거하며,
 *   실패 분류·백오프·FAILED 판정은 폴러(Tx2)의 단일 책임으로 유지
 * - 비동기 실행 (외부 API 호출이므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCompletedEventListener {

    private final OutboxRepository outboxRepository;
    private final OutboxV4TransactionManager outboxTransactionManager;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handle(MissionCompletedEvent event) {
        OutboxEvent outbox = outboxRepository.findById(event.getOutboxId()).orElse(null);
        if (outbox == null) {
            log.warn("outbox 없음, 전송 스킵: outboxId={}", event.getOutboxId());
            return;
        }

        MissionLogPayload payload = parsePayload(outbox);
        if (payload == null) {
            return;
        }

        SendResult result = adminClient.sendMissionLog(payload, outbox.getId());

        if (result instanceof SendResult.Success) {
            outboxTransactionManager.markSent(outbox.getId());
            log.info("미션 완료 로그 전송 성공: outboxId={}", outbox.getId());
        } else {
            log.warn("미션 완료 로그 즉시 전송 실패, 폴러가 재시도 예정: {}", result);
        }
    }

    private MissionLogPayload parsePayload(OutboxEvent outbox) {
        try {
            return objectMapper.readValue(outbox.getPayload(), MissionLogPayload.class);
        } catch (JsonProcessingException e) {
            // 파싱 실패 건은 폴러가 영구 실패(FAILED)로 정리하도록 위임
            log.error("payload 파싱 실패, 폴러가 정리 예정: outboxId={}, error={}",
                    outbox.getId(), e.getMessage());
            return null;
        }
    }
}