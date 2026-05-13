package com.team.peektime_api.global.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000)  // 1분마다
    public void pollAndProcess() {
        // 생성된 지 3초 이상 된 것만 조회 (즉시 처리 중인 것 제외)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        List<OutboxEvent> events = outboxRepository.findByCreatedAtBefore(threshold);

        if (events.isEmpty()) {
            return;
        }

        log.info("Outbox 폴링: {}건 처리 시작", events.size());

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            adminClient.sendMissionLog(payload);
            outboxRepository.delete(event);

            log.info("Outbox 재시도 성공: id={}", event.getId());
        } catch (Exception e) {
            log.error("Outbox 재시도 실패: id={}, error={}", event.getId(), e.getMessage());
        }
    }
}