package com.team.peektime_api.global.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.outbox.SendResult.AlreadyProcessed;
import com.team.peektime_api.global.outbox.SendResult.Success;
import com.team.peektime_api.global.outbox.SendResult.Unknown;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000)  // 1분마다
    @Transactional
    public void pollAndProcess() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        List<OutboxEvent> events = outboxRepository.findByCreatedAtBeforeWithSkipLocked(threshold);

        if (events.isEmpty()) {
            log.info("기록해야 하는 미션이 존재하지 않습니다.");
            return;
        }

        log.info("Outbox 폴링: {}건 처리 시작", events.size());

        ProcessingResult result = processAllEvents(events);


        deleteProcessedEvents(result.toDelete());
        logUnknownEvents(result.unknownCount());
    }




    private ProcessingResult processAllEvents(List<OutboxEvent> events) {
        List<Long> toDelete = new ArrayList<>();
        int unknownCount = 0;

        for (OutboxEvent event : events) {
            SendResult result = processEvent(event);

            if (result instanceof Success s) {
                toDelete.add(s.eventId());
            } else if (result instanceof AlreadyProcessed a) {
                toDelete.add(a.eventId());
            } else if (result instanceof Unknown u) {
                unknownCount++;
                log.warn("Outbox 결과 알 수 없음: id={}, reason={}", u.eventId(), u.reason());
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
            return new Unknown(event.getId(), e.getMessage());
        }
    }



//    @Scheduled(fixedDelay = 60000)
//    public void pollAndProcess() { // @Tx 존재하지 않음
//
//        // 1. [트랜잭션 1] OutBox 테이블에 존재하는 row 가져옴
//        List<OutboxEvent> events = outboxService.조회();
//
//        if (events.isEmpty()) {
//            return;
//        }
//
//        // 2. 외부 API 호출 및 후처리는 트랜잭션 없이(혹은 건별로 따로) 진행합니다.
//        for (OutboxEvent event : events) {
//            processEvent(event);
//        }
//    }
//
//    private void processEvent(OutboxEvent event) {
//        try {
//            MissionLogPayload payload = objectMapper.readValue(event.getPayload(), MissionLogPayload.class);
//
//            // 트랜잭션 밖에서 외부 API 호출 (시간이 오래 걸려도 DB 커넥션을 붙잡지 않음!)
//            adminClient.sendMissionLog(payload);
//
//            // 3. [트랜잭션 2] 성공 시 삭제 작업만 별도 트랜잭션으로 처리
//            outboxService.deleteEvent(event.getId());
//
//            log.info("Outbox 재시도 성공: id={}", event.getId());
//        } catch (Exception e) {
//            log.error("Outbox 재시도 실패: id={}, error={}", event.getId(), e.getMessage());
//        }
//    }

}