package com.team.peektime_api.global.outbox.scheduler.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbox 소비 담당 (Consumer)
 *
 * - Redis SET에서 SPOP으로 ID 획득
 * - 외부 API 호출
 * - 성공/4xx → DB 삭제
 * - 알 수 없는 에러 → DB 유지 (다음 장전에서 재시도)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxConsumer {

    private static final String OUTBOX_QUEUE_KEY = "outbox:set:queue";

    private final RedisTemplate<String, String> redisTemplate;
    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Redis SET이 빌 때까지 소비
     */
    public void consumeAll() {
        int processedCount = 0;
        int deletedCount = 0;

        while (true) {
            String poppedId = redisTemplate.opsForSet().pop(OUTBOX_QUEUE_KEY);
            if (poppedId == null) {
                break;
            }

            processedCount++;
            Long eventId = Long.valueOf(poppedId);

            if (processEvent(eventId)) {
                deletedCount++;
            }
        }

        if (processedCount > 0) {
            log.info("[V3] 소비 완료: 처리 {}건, 삭제 {}건", processedCount, deletedCount);
        }
    }

    /**
     * 단일 이벤트 처리
     *
     * @return true: DB 삭제 완료, false: DB 유지 (재시도 필요)
     */
    private boolean processEvent(Long eventId) {
        OutboxEvent event = outboxRepository.findById(eventId).orElse(null);
        if (event == null) {
            log.warn("[V3] 이벤트 없음 (이미 삭제됨): id={}", eventId);
            return false;
        }

        SendResult result = executeExternalApi(event);

        return switch (result) {
            case SendResult.Success s -> deleteEvent(s.eventId());
            case SendResult.PermanentFailure pf -> {
                log.warn("[V3] 영구 실패 (삭제): id={}, reason={}", pf.eventId(), pf.reason());
                yield deleteEvent(pf.eventId());
            }
            case SendResult.TransientFailure tf -> {
                log.warn("[V3] 일시 실패 (재시도 대기): id={}, reason={}", tf.eventId(), tf.reason());
                yield false;
            }
        };
    }

    private SendResult executeExternalApi(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            return adminClient.sendMissionLog(payload, event.getId());

        } catch (JsonProcessingException e) {
            // 파싱 실패는 재시도해도 같은 결과 → 영구 실패
            log.error("[V3] payload 파싱 실패 (영구 실패): id={}, error={}", event.getId(), e.getMessage());
            return new SendResult.PermanentFailure(event.getId(), "payload 파싱 실패");
        }
    }

    private boolean deleteEvent(Long eventId) {
        try {
            outboxRepository.deleteById(eventId);
            return true;
        } catch (Exception e) {
            log.error("[V3] DB 삭제 실패 (다음 장전에서 재시도): id={}", eventId);
            return false;
        }
    }
}