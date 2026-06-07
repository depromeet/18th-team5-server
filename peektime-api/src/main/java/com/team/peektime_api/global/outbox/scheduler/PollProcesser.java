package com.team.peektime_api.global.outbox.scheduler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollProcesser {


    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;



    @Transactional
    public void process() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        List<OutboxEvent> events = outboxRepository.findByCreatedAtBeforeWithSkipLocked(threshold);

        if (events.isEmpty()) {
            log.info("기록해야 하는 미션이 존재하지 않습니다.");
            return;
        }

        log.info("Outbox 폴링: {}건 처리 시작", events.size());
        ProcessingResult result = processAllEvents(events);

        // TX : 성공시 Outbox 테이블에서 삭제
        deleteProcessedEvents(result.toDelete());
        logUnknownEvents(result.unknownCount());
    }



    private ProcessingResult processAllEvents(List<OutboxEvent> events) {
        List<Long> toDelete = new ArrayList<>();
        int unknownCount = 0;

        for (OutboxEvent event : events) {
            SendResult result = processEvent(event);

            if (result instanceof SendResult.Success s) {
                toDelete.add(s.eventId());
            } else if (result instanceof SendResult.PermanentFailure pf) {
                toDelete.add(pf.eventId());
                log.warn("Outbox 영구 실패 (삭제): id={}, reason={}", pf.eventId(), pf.reason());
            } else if (result instanceof SendResult.TransientFailure tf) {
                unknownCount++;
                log.warn("Outbox 일시 실패 (재시도 대기): id={}, reason={}", tf.eventId(), tf.reason());
            }
        }

        return new ProcessingResult(toDelete, unknownCount);
    }

    private void deleteProcessedEvents(List<Long> ids) {
        if (!ids.isEmpty()) {
            outboxRepository.deleteAllByIdInBatch(ids);
            log.info("Outbox 삭제 완료: {}건", ids.size());
        }
    }

    private void logUnknownEvents(int count) {
        if (count > 0) {
            log.warn("Outbox 재시도 대기: {}건", count);
        }
    }

    private record ProcessingResult(List<Long> toDelete, int unknownCount) {}

    private SendResult processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            return adminClient.sendMissionLogWithResult(payload, event.getId());

        } catch (Exception e) {
            // 파싱 실패 등은 영구 실패로 처리
            return new SendResult.PermanentFailure(event.getId(), e.getMessage());
        }
    }

}
