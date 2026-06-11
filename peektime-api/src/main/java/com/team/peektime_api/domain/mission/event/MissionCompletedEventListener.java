package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

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

            SendResult result = adminClient.sendMissionLog(payload, event.getOutboxId());

            if (result instanceof SendResult.Success) {
                deleteOutbox(event.getOutboxId());
                log.info("미션 완료 로그 전송 성공: missionId={}", completion.getMission().getId());
            } else {
                log.warn("미션 완료 로그 즉시 전송 실패, 폴러가 재시도 예정: {}", result);
            }
        } catch (Exception e) {
            log.warn("미션 완료 로그 즉시 전송 실패, 폴러가 재시도 예정: {}", e.getMessage());
        }
    }

    private UserMissionCompletion findCompletion(Long completionId) {
        return completionRepository.findById(completionId)
                .orElseThrow(() -> new IllegalStateException("completion 없음: id=" + completionId));
    }

    private MissionLogPayload createPayload(UserMissionCompletion completion) {
        Long userId = completion.getUser().getId();
        Long missionId = completion.getMission().getId();
        LocalDate completedDate = completion.getCreatedAt().toLocalDate();
        Long solarTermId = completion.getSolarTerm().getId();

        String idempotencyKey = generateIdempotencyKey(userId, missionId, completedDate);

        return MissionLogPayload.of(idempotencyKey, userId, solarTermId);
    }

    private String generateIdempotencyKey(Long userId, Long missionId, LocalDate completedDate) {
        String raw = userId + ":" + missionId + ":" + completedDate;
        String hash = sha256(raw).substring(0, 8);
        return raw + ":" + hash;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해싱 실패", e);
        }
    }

    private void deleteOutbox(Long outboxId) {
        outboxRepository.deleteById(outboxId);
    }
}