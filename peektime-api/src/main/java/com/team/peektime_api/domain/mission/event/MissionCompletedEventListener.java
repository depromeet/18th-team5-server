package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 미션 완료 이벤트를 받아 Admin 서버로 로그를 전송하는 리스너
 *
 * - Pull 방식: completionId로 필요한 데이터 직접 조회
 * - 비동기 실행 (외부 API 호출이므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCompletedEventListener {

    private final UserMissionCompletionRepository completionRepository;
    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handle(MissionCompletedEvent event) {
        try {
            UserMissionCompletion completion = findCompletion(event.getCompletionId());
            MissionLogPayload payload = createPayload(completion);

            sendLogToAdmin(payload);
            deleteOutbox(event.getOutboxId());

            log.info("미션 완료 로그 전송 성공: missionId={}", completion.getMission().getId());
        } catch (Exception e) {
            log.warn("미션 완료 로그 즉시 전송 실패, 폴러가 재시도 예정: {}", e.getMessage());
        }
    }

    private UserMissionCompletion findCompletion(Long completionId) {
        return completionRepository.findById(completionId)
                .orElseThrow(() -> new IllegalStateException("completion 없음: id=" + completionId));
    }

    private MissionLogPayload createPayload(UserMissionCompletion completion) {
        return MissionLogPayload.of(
                completion.getUser().getDeviceUuid(),
                completion.getMission().getId(),
                completion.getMissionType(),
                completion.getSolarTerm().getId(),
                completion.getCreatedAt()
        );
    }

    private void sendLogToAdmin(MissionLogPayload payload) {
        adminClient.sendMissionLog(payload);
    }

    private void deleteOutbox(Long outboxId) {
        outboxRepository.deleteById(outboxId);
    }
}