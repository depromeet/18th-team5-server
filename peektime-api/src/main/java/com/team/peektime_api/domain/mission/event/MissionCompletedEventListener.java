package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCompletedEventListener {

    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handle(MissionCompletedEvent event) {
        try {
            MissionLogPayload payload = MissionLogPayload.from(event);
            adminClient.sendMissionLog(payload);
            outboxRepository.deleteById(event.getOutboxId());

            log.info("미션 완료 로그 전송 성공: missionId={}", event.getMissionId());
        } catch (Exception e) {
            log.warn("미션 완료 로그 즉시 전송 실패, 폴러가 재시도 예정: {}", e.getMessage());
        }
    }
}